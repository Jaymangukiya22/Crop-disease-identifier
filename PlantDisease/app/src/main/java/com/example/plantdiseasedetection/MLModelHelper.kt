// File: /Users/jay/Documents/Sem7/MPC/Project/Crop-disease-identifier/PlantDisease/app/src/main/java/com/example/plantdiseasedetection/MLModelHelper.kt
package com.example.plantdiseasedetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import java.util.Collections

class MLModelHelper(private val context: Context) {

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    // Constants for image preprocessing
    companion object {
        private const val TAG = "MLModelHelper"
        private const val MODEL_PATH = "plant_disease.ort" // Your ONNX model file
        private const val INPUT_SIZE = 224 // Standard input size for most plant disease models
        private const val CHANNELS = 3 // RGB color channels (NOT grayscale)
        // ImageNet normalization values - standard for most plant disease models
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f) // RGB mean values
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)  // RGB std values
    }

    private var isModelInitialized = false
    private var modelLoadError: String? = null
    private var labels: List<String> = emptyList()

    init {
        // Initialize ONNX Runtime environment and session
        ortEnvironment = OrtEnvironment.getEnvironment()
        ortSession = createOrtSession()
    }

    private fun createOrtSession(): OrtSession? {
        return ortEnvironment?.let { env ->
            try {
                Log.d(TAG, "🔄 Loading ONNX model from assets: $MODEL_PATH")
                val modelBytes = context.assets.open(MODEL_PATH).readBytes()
                Log.d(TAG, "✅ Model loaded successfully. Size: ${modelBytes.size} bytes")

                // Load labels
                labels = loadDiseaseLabels()
                Log.d(TAG, "✅ Labels loaded: ${labels.size} classes")
                labels.forEachIndexed { index, label ->
                    Log.d(TAG, "Class $index: $label")
                }

                val session = env.createSession(modelBytes, OrtSession.SessionOptions())
                isModelInitialized = true
                modelLoadError = null
                Log.d(TAG, "✅ ONNX Runtime session created successfully")
                session
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to initialize ONNX model: ${e.message}", e)
                isModelInitialized = false
                modelLoadError = e.message
                null
            }
        }
    }

    /**
     * Performs disease detection on the given bitmap.
     * This function should be called from a background thread.
     */
    suspend fun detectDisease(bitmap: Bitmap): DetectionResult {
        if (!isModelInitialized || ortSession == null || ortEnvironment == null) {
            throw Exception("❌ Model not initialized. Error: ${modelLoadError ?: "Unknown error during initialization"}")
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔍 Starting disease detection...")
                Log.d(TAG, "📸 Input image size: ${bitmap.width}x${bitmap.height}")
                Log.d(TAG, "🎨 Image format: ${bitmap.config} (supports RGB color images)")

                // Preprocess the image
                val inputTensor = preprocessImage(bitmap)
                Log.d(TAG, "✅ Image preprocessed to ${INPUT_SIZE}x${INPUT_SIZE} RGB channels")

                // Run inference
                val inputName = ortSession!!.inputNames.iterator().next() // Safe call due to isModelInitialized check
                Log.d(TAG, "🧠 Running ONNX inference with input: $inputName")
                val inputs = Collections.singletonMap(inputName, inputTensor)
                val outputs = ortSession!!.run(inputs) // Safe call

                // Postprocess the output
                outputs.use { // Use .use for AutoCloseable
                    @Suppress("UNCHECKED_CAST")
                    val outputArray = (outputs[0].value as Array<FloatArray>)[0]
                    Log.d(TAG, "📊 Model output array size: ${outputArray.size}")

                    // Get sorted indices as a List<Int>
                    val sortedIndicesList = outputArray.indices.sortedByDescending { outputArray[it] }

                    Log.d(TAG, "🏆 Top 5 predictions:")
                    for (i in 0 until minOf(5, sortedIndicesList.size)) {
                        val idx = sortedIndicesList[i]
                        val className = if (idx < labels.size) labels[idx] else "Unknown_$idx"
                        Log.d(TAG, "  ${i+1}. $className: ${String.format("%.4f", outputArray[idx])}")
                    }

                    if (sortedIndicesList.isEmpty()) {
                        throw Exception("Model output is empty or invalid.")
                    }

                    val maxIdx = sortedIndicesList[0]
                    val confidence = outputArray[maxIdx]
                    val detectedDiseaseName = if (maxIdx < labels.size) labels[maxIdx] else "Unknown_$maxIdx"

                    Log.d(TAG, "🎯 Final prediction: $detectedDiseaseName (confidence: ${String.format("%.4f", confidence)})")

                    // ACCURACY IMPROVEMENT #2: Multi-criteria confidence assessment
                    val enhancedConfidence = calculateEnhancedConfidence(outputArray, sortedIndicesList, labels)
                    val biasAnalysis = analyzePredictionBias(outputArray, sortedIndicesList.toIntArray(), labels)

                    val finalPrediction = if (enhancedConfidence < 0.4f || biasAnalysis.isBiased) {
                        "Uncertain - Please retake photo"
                    } else {
                        detectedDiseaseName
                    }

                    val (cause, prevention, severity) = if (finalPrediction.contains("Uncertain")) {
                        Triple(
                            "Low confidence or biased prediction detected",
                            "Please take a clearer, well-lit photo of the leaf. Ensure the leaf fills most of the frame.",
                            "N/A"
                        )
                    } else {
                        getDiseaseInfo(detectedDiseaseName, confidence)
                    }

                    val result = DetectionResult(
                        diseaseName = finalPrediction,
                        confidence = confidence,
                        cause = cause,
                        prevention = prevention,
                        severity = severity,
                        // Fix: Use sorted predictions, not raw array order
                        rawPredictions = sortedIndicesList.take(5).map { idx -> 
                            String.format("%.4f", outputArray[idx]) 
                        },
                        topClasses = sortedIndicesList.take(5).map { idx ->
                            if (idx < labels.size) labels[idx] else "Unknown_$idx"
                        },
                        isBiasedPrediction = biasAnalysis.isBiased,
                        predictionAnalysis = biasAnalysis.analysis
                    )

                    // Cleanup inputTensor, outputs is handled by .use block
                    inputTensor.close()

                    Log.d(TAG, "✅ Detection completed successfully")
                    result
                } ?: throw Exception("Failed to get model output (outputs were null).")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during disease detection: ${e.message}", e)
                // Ensure the exception is re-thrown to be caught by the calling coroutine
                throw Exception("Error during disease detection: ${e.message}", e)
            }
        }
    }

    /**
     * Enhanced image preprocessing for better accuracy
     */
    private fun preprocessImage(bitmap: Bitmap): OnnxTensor {
        // 1. Apply image enhancement before resizing
        val enhancedBitmap = enhanceImageQuality(bitmap)
        
        // 2. Resize with high-quality filtering
        val resizedBitmap = Bitmap.createScaledBitmap(enhancedBitmap, INPUT_SIZE, INPUT_SIZE, true)

        // 2. Convert bitmap to FloatBuffer and normalize
        val floatBuffer = FloatBuffer.allocate(CHANNELS * INPUT_SIZE * INPUT_SIZE)
        floatBuffer.rewind()

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

        for (i in 0 until INPUT_SIZE * INPUT_SIZE) {
            val pixel = intValues[i]
            // Extract RGB, normalize, and put into buffer in NCHW (Batch, Channels, Height, Width) format
            // Assuming model expects BGR, if RGB then adjust order for MEAN and STD if they are BGR specific
            // For NCHW, fill channels for one pixel, then next pixel etc.
            // Example: For CHW (which is what we want for a single image, batch dim is 1 later)
            // Pixel 0: R0, G0, B0
            // Pixel 1: R1, G1, B1
            // FloatBuffer should be: R0,G0,B0, R1,G1,B1 ... for HWC direct buffer fill
            // OR for CHW: R0,R1,... G0,G1,... B0,B1,...
            // Current ONNX standard is NCHW. The loop below populates in HWC order into the buffer,
            // which needs to be reshaped or transposed if the model strictly expects CHW in that order.
            // However, ONNX runtime CreateTensor for FloatBuffer with shape (1, C, H, W) will
            // interpret the flat FloatBuffer data in that order. So the loop needs to fill CHW.

            // To fill in CHW order directly:
            // This loop structure is incorrect for direct CHW filling.
            // It should be three separate loops or a calculation for indices.
            // A simpler way for this loop is to fill HWC and rely on ONNX interpretation
            // or perform a transpose if needed.
            // Current loop fills HWC:
            // floatBuffer.put(((pixel shr 16 and 0xFF) / 255.0f - MEAN[0]) / STD[0]) // R
            // floatBuffer.put(((pixel shr 8 and 0xFF) / 255.0f - MEAN[1]) / STD[1])  // G
            // floatBuffer.put(((pixel and 0xFF) / 255.0f - MEAN[2]) / STD[2])        // B

            // Corrected loop for CHW planar format:
            // This requires changing how data is put into floatBuffer.
            // Let's assume the previous HWC-like filling was intended and the ONNX runtime handles it
            // with the (1,C,H,W) shape. If not, this part needs careful review based on model specifics.
            // For now, retaining the original HWC-style fill logic as the ONNX `CreateTensor` will take the flat buffer
            // and interpret it according to the provided shape (1, C, H, W).
            // This means the buffer should contain all Red channel pixels, then all Green, then all Blue.

        }
        // Correct way to fill for NCHW (N=1)
        for (c in 0 until CHANNELS) {
            for (h in 0 until INPUT_SIZE) {
                for (w in 0 until INPUT_SIZE) {
                    val pixelIndex = h * INPUT_SIZE + w
                    val pixel = intValues[pixelIndex]
                    val value = when (c) {
                        0 -> (pixel shr 16 and 0xFF) / 255.0f // Red
                        1 -> (pixel shr 8 and 0xFF) / 255.0f  // Green
                        else -> (pixel and 0xFF) / 255.0f     // Blue
                    }
                    floatBuffer.put((value - MEAN[c]) / STD[c])
                }
            }
        }
        floatBuffer.rewind()

        // Create tensor
        val shape = longArrayOf(1, CHANNELS.toLong(), INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        return OnnxTensor.createTensor(ortEnvironment!!, floatBuffer, shape) // Safe call due to isModelInitialized check
    }

    /**
     * ACCURACY IMPROVEMENT #1: Image Quality Enhancement
     * Enhances image quality before preprocessing to improve recognition rates
     */
    private fun enhanceImageQuality(bitmap: Bitmap): Bitmap {
        val canvas = Canvas()
        val enhancedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(enhancedBitmap)
        
        // Apply color enhancement for better plant feature detection
        val colorMatrix = ColorMatrix().apply {
            // Increase contrast and saturation for better leaf details
            setSaturation(1.2f) // 20% more saturation
            val contrastMatrix = ColorMatrix(floatArrayOf(
                1.1f, 0f, 0f, 0f, 10f,    // Red: +10% contrast, +10 brightness
                0f, 1.1f, 0f, 0f, 10f,    // Green: +10% contrast, +10 brightness  
                0f, 0f, 1.1f, 0f, 10f,    // Blue: +10% contrast, +10 brightness
                0f, 0f, 0f, 1f, 0f        // Alpha unchanged
            ))
            postConcat(contrastMatrix)
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        Log.d(TAG, "🎨 Applied image enhancement: +20% saturation, +10% contrast")
        return enhancedBitmap
    }

    /**
     * ACCURACY IMPROVEMENT #2: Enhanced Confidence Calculation
     * Uses multiple metrics to assess prediction reliability
     */
    private fun calculateEnhancedConfidence(outputArray: FloatArray, sortedIndices: List<Int>, labels: List<String>): Float {
        if (sortedIndices.isEmpty()) return 0f
        
        val topConfidence = outputArray[sortedIndices[0]]
        val secondConfidence = if (sortedIndices.size > 1) outputArray[sortedIndices[1]] else 0f
        val thirdConfidence = if (sortedIndices.size > 2) outputArray[sortedIndices[2]] else 0f
        
        // Calculate confidence gap (separation between top predictions)
        val confidenceGap = topConfidence - secondConfidence
        
        // Calculate prediction entropy (diversity measure)
        val top5Values = sortedIndices.take(5).map { outputArray[it] }
        val totalTop5 = top5Values.sum()
        val entropy = if (totalTop5 > 0) {
            -top5Values.sumOf { value ->
                val p = value / totalTop5
                if (p > 0) p * kotlin.math.ln(p.toDouble()) else 0.0
            }
        } else 0.0
        
        // Enhanced confidence formula combining multiple factors
        val enhancedConfidence = when {
            // High confidence with good separation
            topConfidence > 0.8f && confidenceGap > 0.3f -> topConfidence * 1.1f
            // Good confidence with reasonable separation  
            topConfidence > 0.6f && confidenceGap > 0.2f -> topConfidence * 1.05f
            // Penalize low diversity (concentrated predictions)
            entropy < 0.5 -> topConfidence * 0.9f
            // Penalize very close predictions
            confidenceGap < 0.1f -> topConfidence * 0.8f
            else -> topConfidence
        }.coerceIn(0f, 1f)
        
        Log.d(TAG, "📈 Enhanced confidence: ${String.format("%.4f", enhancedConfidence)} " +
                   "(raw: ${String.format("%.4f", topConfidence)}, gap: ${String.format("%.4f", confidenceGap)}, " +
                   "entropy: ${String.format("%.3f", entropy)})")
        
        return enhancedConfidence
    }

    /**
     * Analyze prediction for bias towards specific classes with improved algorithm
     */
    private fun analyzePredictionBias(outputArray: FloatArray, sortedIndices: IntArray, labels: List<String>): BiasAnalysis {
        if (sortedIndices.isEmpty()) {
            return BiasAnalysis(isBiased = false, analysis = "Bias Analysis: No predictions to analyze.")
        }
        
        val topPredictionValue = outputArray[sortedIndices[0]]
        val secondPredictionValue = if (sortedIndices.size > 1) outputArray[sortedIndices[1]] else 0f
        val thirdPredictionValue = if (sortedIndices.size > 2) outputArray[sortedIndices[2]] else 0f
        val confidenceGap = topPredictionValue - secondPredictionValue

        val topClassName = if (sortedIndices[0] < labels.size) labels[sortedIndices[0]] else "Unknown"
        
        // Get top 5 class names for analysis
        val top5Classes = sortedIndices.take(5).map { idx ->
            if (idx < labels.size) labels[idx] else "Unknown"
        }

        // Improved bias detection patterns
        val cornRelatedCount = top5Classes.count { className ->
            className.contains("Corn", ignoreCase = true) || 
            className.contains("Maize", ignoreCase = true)
        }
        
        // Calculate prediction diversity (entropy-like measure)
        val top5Values = sortedIndices.take(5).map { outputArray[it] }
        val totalTop5 = top5Values.sum()
        val diversity = if (totalTop5 > 0) {
            -top5Values.sumOf { value ->
                val p = value / totalTop5
                if (p > 0) p * kotlin.math.ln(p.toDouble()) else 0.0
            }
        } else 0.0

        // More conservative bias detection logic
        val isBiased = when {
            // Case 1: Only flag extreme overfitting (99%+ confidence with huge gap)
            topPredictionValue > 0.99f && confidenceGap > 0.95f -> {
                Log.d(TAG, "Bias detected: Extreme overfitting pattern")
                true
            }
            
            // Case 2: Only flag if ALL top 5 are corn-related (very suspicious)
            cornRelatedCount >= 5 && topPredictionValue > 0.8f -> {
                Log.d(TAG, "Bias detected: Complete corn dominance in all top 5")
                true
            }
            
            // Case 3: Only flag extremely low diversity with very high confidence
            diversity < 0.1 && topPredictionValue > 0.95f -> {
                Log.d(TAG, "Bias detected: Extremely low prediction diversity")
                true
            }
            
            // Case 4: Only flag very suspicious corn patterns
            (topClassName.contains("Corn", ignoreCase = true) || topClassName.contains("Maize", ignoreCase = true)) &&
            topPredictionValue > 0.9f && confidenceGap > 0.8f && 
            secondPredictionValue < 0.05f && cornRelatedCount >= 4 -> {
                Log.d(TAG, "Bias detected: Very suspicious corn prediction pattern")
                true
            }
            
            else -> {
                Log.d(TAG, "No bias detected - prediction appears legitimate")
                false
            }
        }

        val analysis = buildString {
            appendLine("🔍 ENHANCED BIAS ANALYSIS:")
            appendLine("Top prediction: $topClassName (${String.format("%.4f", topPredictionValue)})")
            appendLine("Confidence gap: ${String.format("%.4f", confidenceGap)}")
            appendLine("Corn-related in top 5: $cornRelatedCount/5")
            appendLine("Prediction diversity: ${String.format("%.3f", diversity)}")
            appendLine("Second best: ${String.format("%.4f", secondPredictionValue)}")
            appendLine("Third best: ${String.format("%.4f", thirdPredictionValue)}")
            appendLine()
            appendLine("🎯 Top 5 classes:")
            top5Classes.forEachIndexed { index, className ->
                val value = if (index < top5Values.size) String.format("%.4f", top5Values[index]) else "N/A"
                val indicator = if (className.contains("Corn", ignoreCase = true) || 
                                   className.contains("Maize", ignoreCase = true)) "🌽" else "🌿"
                appendLine("  ${index + 1}. $indicator $className: $value")
            }
            appendLine()
            appendLine("🚨 BIAS DETECTED: $isBiased")
            if (isBiased) {
                appendLine("⚠️ Recommendation: Retake photo with better lighting/angle")
            }
        }

        Log.d(TAG, analysis)
        return BiasAnalysis(isBiased, analysis)
    }

    data class BiasAnalysis(
        val isBiased: Boolean,
        val analysis: String
    )

    /**
     * Load disease labels from assets
     */
    private fun loadDiseaseLabels(): List<String> {
        return try {
            context.assets.open("labels.txt").use { inputStream ->
                inputStream.bufferedReader().readLines().map { it.trim() }.filter { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load labels: ${e.message}", e)
            // Return a default list or throw an error to indicate critical failure
            listOf("Error: Could not load labels")
        }
    }

    /**
     * Get detailed disease information based on disease name and confidence
     */
    private fun getDiseaseInfo(diseaseName: String, confidence: Float): Triple<String, String, String> {
        val severity = when {
            confidence > 0.85f -> "High"
            confidence > 0.6f -> "Moderate"
            else -> "Low"
        }

        // Example: This should ideally come from a more structured data source
        return when {
            diseaseName.contains("Healthy", ignoreCase = true) -> Triple(
                "No significant disease detected. The plant appears to be healthy.",
                "Maintain good plant hygiene. Continue regular monitoring for any signs of stress or disease. Ensure proper watering and fertilization according to plant needs.",
                "None"
            )
            diseaseName.contains("Scab", ignoreCase = true) -> Triple(
                "Caused by the fungus Venturia inaequalis. Thrives in cool, wet spring weather. Symptoms include dark, scabby lesions on leaves and fruit.",
                "Remove and destroy infected leaves and fruit. Apply appropriate fungicides starting early in the season. Prune trees to improve air circulation. Rake up fallen leaves in autumn.",
                severity
            )
            diseaseName.contains("Black Rot", ignoreCase = true) -> Triple(
                "Fungal disease caused by Botryosphaeria obtusa. Affects fruit, leaves, and wood. Symptoms include fruit rot, leaf spots, and cankers on branches.",
                "Prune out and destroy infected plant parts, including cankered limbs. Apply fungicides during the growing season. Ensure good air circulation and sunlight penetration.",
                severity
            )
            diseaseName.contains("Rust", ignoreCase = true) -> Triple(
                "Caused by various species of fungi. Characterized by reddish-orange pustules on leaves and stems. Can reduce plant vigor and yield.",
                "Remove and destroy infected plant material. Apply fungicides if necessary. Some rusts require an alternate host; removing it can break the disease cycle. Improve air circulation.",
                severity
            )
            // Add more specific disease information here
            else -> Triple(
                "Information for '$diseaseName' is not available in the local database.",
                "Consult agricultural extension services or online plant pathology resources for more information about this specific condition.",
                severity
            )
        }
    }

    /**
     * Releases resources used by the ONNX Runtime.
     * Call this when the model is no longer needed.
     */
    fun release() {
        Log.d(TAG, "Releasing ONNX Runtime resources.")
        try {
            ortSession?.close()
            ortEnvironment?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing ONNX resources: ${e.message}", e)
        } finally {
            ortSession = null
            ortEnvironment = null
            isModelInitialized = false
        }
    }

    /**
     * Get model status information
     */
    fun getModelStatus(): ModelStatus {
        return ModelStatus(
            isInitialized = isModelInitialized,
            error = modelLoadError,
            totalClasses = labels.size,
            modelPath = MODEL_PATH
        )
    }

    data class ModelStatus(
        val isInitialized: Boolean,
        val error: String?,
        val totalClasses: Int,
        val modelPath: String
    )

    // Data class for detection results
    data class DetectionResult(
        val diseaseName: String,
        val confidence: Float,
        val cause: String,
        val prevention: String,
        val severity: String,
        val rawPredictions: List<String>, // List of top raw prediction scores as strings
        val topClasses: List<String>,     // List of top class names
        val isBiasedPrediction: Boolean,
        val predictionAnalysis: String
    )
}
