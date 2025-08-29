package com.example.plantdiseasedetection

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * DetectionActivity - Plant Disease Detection Page
 * 
 * This activity handles image capture from camera, image selection from gallery,
 * and displays disease detection results (currently placeholder values).
 * 
 * TODO: Integrate ML model here to replace placeholder disease detection
 */
class DetectionActivity : AppCompatActivity() {

    // UI Components
    private lateinit var btnCaptureImage: MaterialButton
    private lateinit var btnUploadImage: MaterialButton
    private lateinit var btnHistory: MaterialButton
    private lateinit var ivPlantImage: ImageView
    private lateinit var tvImageStatus: TextView
    private lateinit var tvDebugInfo: TextView
    private lateinit var tvDiseaseName: TextView
    private lateinit var tvCause: TextView
    private lateinit var tvPrevention: TextView

    // Image handling
    private var currentImagePath: String? = null
    private var currentImageUri: Uri? = null
    private var currentBitmap: Bitmap? = null
    private var lastSourceType: SessionHistory.SourceType? = null

    // ML Model helper
    private lateinit var mlModelHelper: MLModelHelper

    // Permission request codes
    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val STORAGE_PERMISSION_REQUEST = 101
        private const val GALLERY_REQUEST_CODE = 201
    }

    // Activity result launchers
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleCameraResult()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleGalleryResult(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection)

        // Initialize ML model helper
        mlModelHelper = MLModelHelper(this)

        // Initialize UI components
        initializeViews()
        setupClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release ML model resources
        mlModelHelper.release()
    }

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        btnCaptureImage = findViewById(R.id.btn_capture_image)
        btnUploadImage = findViewById(R.id.btn_upload_image)
        btnHistory = findViewById(R.id.btn_history)
        ivPlantImage = findViewById(R.id.iv_plant_image)
        tvImageStatus = findViewById(R.id.tv_image_status)
        tvDebugInfo = findViewById(R.id.tv_debug_info)
        tvDiseaseName = findViewById(R.id.tv_disease_name)
        tvCause = findViewById(R.id.tv_cause)
        tvPrevention = findViewById(R.id.tv_prevention)

        // Toggle history button based on current session entries
        btnHistory.visibility = if (SessionHistory.getAll().isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    /**
     * Set up click listeners for buttons
     */
    private fun setupClickListeners() {
        btnCaptureImage.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        btnUploadImage.setOnClickListener {
            if (checkStoragePermission()) {
                openGallery()
            } else {
                requestStoragePermission()
            }
        }

        btnHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Check if camera permission is granted
     */
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request camera permission
     */
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }

    /**
     * Check if storage permission is granted
     */
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request storage permission
     */
    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        ActivityCompat.requestPermissions(
            this,
            arrayOf(permission),
            STORAGE_PERMISSION_REQUEST
        )
    }

    /**
     * Open camera to capture image
     */
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val photoFile = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(this, getString(R.string.error_capturing_image), Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.let { file ->
            currentImagePath = file.absolutePath
            currentImageUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri)
            cameraLauncher.launch(intent)
            lastSourceType = SessionHistory.SourceType.CAMERA
        }
    }

    /**
     * Open gallery to select image
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
        lastSourceType = SessionHistory.SourceType.GALLERY
    }

    /**
     * Create a temporary image file for camera capture
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    /**
     * Handle camera capture result
     */
    private fun handleCameraResult() {
        currentImagePath?.let { path ->
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(path, options)
                val target = 1280
                var sample = 1
                while (options.outWidth / sample > target || options.outHeight / sample > target) {
                    sample *= 2
                }
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = BitmapFactory.decodeFile(path, decodeOptions)
                if (bitmap != null) {
                    displayImage(bitmap)
                    performDiseaseDetection()
                } else {
                    Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Handle gallery selection result
     */
    private fun handleGalleryResult(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                stream.mark(Int.MAX_VALUE)
                BitmapFactory.decodeStream(stream, null, options)
                try { stream.reset() } catch (_: Exception) {}

                val target = 1280
                var sample = 1
                while (options.outWidth / sample > target || options.outHeight / sample > target) {
                    sample *= 2
                }

                contentResolver.openInputStream(uri)?.use { stream2 ->
                    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
                    val bitmap = BitmapFactory.decodeStream(stream2, null, decodeOptions)
                    if (bitmap != null) {
                        currentImageUri = uri
                        displayImage(bitmap)
                        performDiseaseDetection()
                    } else {
                        Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Display the captured/selected image
     */
    private fun displayImage(bitmap: Bitmap) {
        currentBitmap = bitmap
        ivPlantImage.setImageBitmap(bitmap)
        tvImageStatus.text = "Image loaded successfully"
    }

    /**
     * Perform disease detection using ML model
     * 
     * This method integrates with the MLModelHelper to perform actual disease detection
     */
    private fun performDiseaseDetection() {
        // Show loading state
        tvImageStatus.text = "Analyzing image..."

        // Use the stored bitmap for analysis
        currentBitmap?.let { bitmap ->
            // Launch a coroutine to call the suspend function
            lifecycleScope.launch {
                try {
                    val result = mlModelHelper.detectDisease(bitmap) // Call the suspend function directly
                    // Update UI on the main thread
                    updateDiseaseInfo(result)
                    updateDebugInfo(result) // Add this line to show debug predictions
                    tvImageStatus.text = "Analysis complete (${(result.confidence * 100).toInt()}% confidence)"
                } catch (e: Exception) {
                    // Handle errors, for example, show a toast
                    Toast.makeText(this@DetectionActivity, "Error detecting disease: ${e.message}", Toast.LENGTH_LONG).show()
                    tvImageStatus.text = "Analysis failed"
                    tvDebugInfo.text = "❌ Error: ${e.message}"
                }
            }
        } ?: run {
            // Fallback to placeholder if no image
            val diseaseResult = getPlaceholderDiseaseResult()
            updateDiseaseInfo(diseaseResult)
            updateDebugInfo(diseaseResult) // Add this line for placeholder debug info
            tvDebugInfo.text = "⚠️ Using placeholder data (no image provided)"
        }
    }

    /**
     * Get placeholder disease detection results
     * 
     * TODO: Replace with actual ML model prediction
     */
    private fun getPlaceholderDiseaseResult(): MLModelHelper.DetectionResult {
        // Simulate different disease results based on image
        val diseases = listOf(
            MLModelHelper.DetectionResult(
                "Leaf Blight",
                0.85f,
                "Fungal infection caused by Alternaria alternata",
                "Apply fungicide, improve air circulation, remove infected leaves",
                "Moderate",
                listOf("0.8500", "0.1200", "0.0300"),
                listOf("Leaf Blight", "Powdery Mildew", "Healthy"),
                false,
                "Placeholder prediction analysis"
            ),
            MLModelHelper.DetectionResult(
                "Powdery Mildew",
                0.92f,
                "Fungal disease caused by Erysiphe cichoracearum",
                "Apply sulfur-based fungicide, increase plant spacing, improve ventilation",
                "High",
                listOf("0.9200", "0.0500", "0.0300"),
                listOf("Powdery Mildew", "Leaf Blight", "Healthy"),
                false,
                "Placeholder prediction analysis"
            ),
            MLModelHelper.DetectionResult(
                "Bacterial Spot",
                0.78f,
                "Bacterial infection caused by Xanthomonas campestris",
                "Remove infected plants, apply copper-based bactericide, avoid overhead watering",
                "Moderate",
                listOf("0.7800", "0.1500", "0.0700"),
                listOf("Bacterial Spot", "Leaf Blight", "Healthy"),
                false,
                "Placeholder prediction analysis"
            )
        )
        
        return diseases.random() // Random selection for demo
    }

    /**
     * Update the disease information display
     */
    private fun updateDiseaseInfo(diseaseResult: MLModelHelper.DetectionResult) {
        tvDiseaseName.text = diseaseResult.diseaseName
        tvCause.text = diseaseResult.cause
        tvPrevention.text = diseaseResult.prevention

        // Save to in-memory session history
        // Safely add to session history to avoid crashes
        try {
            val source = lastSourceType ?: SessionHistory.SourceType.GALLERY
            SessionHistory.add(
                bitmap = currentBitmap,
                sourceType = source,
                diseaseName = diseaseResult.diseaseName,
                confidence = diseaseResult.confidence,
                cause = diseaseResult.cause,
                prevention = diseaseResult.prevention
            )
            // Show button after the first successful save
            btnHistory.visibility = android.view.View.VISIBLE
        } catch (e: Exception) {
            // Do not crash app if history save fails for any reason
        }
    }

    /**
     * Update debug information display with confidence priority indicators
     */
    private fun updateDebugInfo(result: MLModelHelper.DetectionResult) {
        val debugText = buildString {
            appendLine("🎯 FINAL PREDICTION:")
            appendLine("${result.diseaseName} (${String.format("%.2f", result.confidence * 100)}%)")
            
            if (result.isBiasedPrediction) {
                appendLine()
                appendLine("⚠️ BIAS DETECTED!")
                appendLine("Model may be overfitted to corn/maize")
            }
            
            appendLine()
            appendLine("🏆 TOP 5 PREDICTIONS (CONFIDENCE PRIORITY):")
            
            // Find the highest confidence prediction
            val confidenceValues = result.rawPredictions.map { it.toFloatOrNull() ?: 0f }
            val maxConfidenceIndex = confidenceValues.indexOf(confidenceValues.maxOrNull() ?: 0f)
            
            result.topClasses.forEachIndexed { index, className ->
                val confidence = if (index < result.rawPredictions.size) result.rawPredictions[index] else "N/A"
                val confidenceFloat = confidence.toFloatOrNull() ?: 0f
                
                // Plant type emoji
                val plantEmoji = getPlantEmoji(className)
                
                // Priority indicator based on confidence
                val priorityIndicator = when {
                    index == maxConfidenceIndex -> "🥇 HIGHEST CONFIDENCE"
                    index == 1 -> "🥈 2nd Choice"
                    index == 2 -> "🥉 3rd Choice"
                    else -> "   Alternative"
                }
                
                // Confidence bar visualization
                val confidenceBar = createConfidenceBar(confidenceFloat)
                
                // Special formatting for highest confidence
                if (index == maxConfidenceIndex) {
                    appendLine("┌─────────────────────────────────────┐")
                    appendLine("│ $priorityIndicator │")
                    appendLine("│ $plantEmoji $className")
                    appendLine("│ Confidence: $confidence ($confidenceBar)")
                    appendLine("└─────────────────────────────────────┘")
                } else {
                    appendLine("${index + 1}. $plantEmoji $className")
                    appendLine("   Confidence: $confidence ($confidenceBar)")
                    appendLine("   $priorityIndicator")
                }
                appendLine()
            }
            
            appendLine("📊 Model's Top Priority: ${result.topClasses.getOrNull(maxConfidenceIndex) ?: "Unknown"}")
            appendLine("🔍 Confidence threshold: 0.30 (30%)")
            appendLine("📈 ${result.predictionAnalysis}")
        }
        tvDebugInfo.text = debugText
    }
    
    /**
     * Get appropriate emoji for plant type
     */
    private fun getPlantEmoji(className: String): String {
        return when {
            className.contains("Corn", ignoreCase = true) || 
            className.contains("Maize", ignoreCase = true) -> "🌽"
            className.contains("Tomato", ignoreCase = true) -> "🍅"
            className.contains("Apple", ignoreCase = true) -> "🍎"
            className.contains("Grape", ignoreCase = true) -> "🍇"
            className.contains("Orange", ignoreCase = true) -> "🍊"
            className.contains("Strawberry", ignoreCase = true) -> "🍓"
            className.contains("Blueberry", ignoreCase = true) -> "🫐"
            className.contains("Raspberry", ignoreCase = true) -> "🫐"
            className.contains("Cherry", ignoreCase = true) -> "🍒"
            className.contains("Peach", ignoreCase = true) -> "🍑"
            className.contains("Potato", ignoreCase = true) -> "🥔"
            className.contains("Pepper", ignoreCase = true) -> "🌶️"
            className.contains("Squash", ignoreCase = true) -> "🎃"
            className.contains("Bean", ignoreCase = true) -> "🫘"
            className.contains("Healthy", ignoreCase = true) -> "✅"
            className.contains("Disease", ignoreCase = true) -> "🦠"
            className.contains("Blight", ignoreCase = true) -> "🍂"
            className.contains("Rust", ignoreCase = true) -> "🟤"
            className.contains("Spot", ignoreCase = true) -> "🔴"
            else -> "🌿"
        }
    }
    
    /**
     * Create visual confidence bar
     */
    private fun createConfidenceBar(confidence: Float): String {
        val percentage = (confidence * 100).toInt()
        val barLength = 10
        val filledBars = (confidence * barLength).toInt().coerceIn(0, barLength)
        val emptyBars = barLength - filledBars
        
        return buildString {
            repeat(filledBars) { append("█") }
            repeat(emptyBars) { append("░") }
            append(" ${percentage}%")
        }
    }

    /**
     * Handle permission request results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
                }
            }
            STORAGE_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, getString(R.string.storage_permission_required), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

} 