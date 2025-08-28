package com.example.plantdiseasedetection

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plantdiseasedetection.adapter.HistoryAdapter
import com.example.plantdiseasedetection.model.DetectionHistory
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton
import java.io.ByteArrayOutputStream
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
    private lateinit var tvDiseaseName: TextView
    private lateinit var tvCause: TextView
    private lateinit var tvPrevention: TextView

    // Current state
    private var currentImagePath: String? = null
    private var currentImageUri: Uri? = null
    private var currentBitmap: Bitmap? = null
    
    // History
    private val detectionHistory = mutableListOf<DetectionHistory>()
    private lateinit var historyAdapter: HistoryAdapter

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
        
        // Initialize history adapter
        historyAdapter = HistoryAdapter(
            onItemClick = { historyItem ->
                // When a history item is clicked, load it into the main view
                loadHistoryItem(historyItem)
            }
        )

        // Set up click listeners
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
        tvDiseaseName = findViewById(R.id.tv_disease_name)
        tvCause = findViewById(R.id.tv_cause)
        tvPrevention = findViewById(R.id.tv_prevention)
        
        // Add history button to the toolbar
        findViewById<MaterialButton>(R.id.btn_history).setOnClickListener {
            showHistoryDialog()
        }
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
            showHistoryDialog()
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
        }
    }

    /**
     * Open gallery to select image
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
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
            val bitmap = BitmapFactory.decodeFile(path)
            displayImage(bitmap)
            performDiseaseDetection() // TODO: Replace with actual ML model call
        }
    }

    /**
     * Handle gallery selection result
     */
    private fun handleGalleryResult(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                displayImage(bitmap)
                performDiseaseDetection() // TODO: Replace with actual ML model call
            } else {
                Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show()
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
                    tvImageStatus.text = "Analysis complete (${(result.confidence * 100).toInt()}% confidence)"
                    
                    // Add to history
                    addToHistory(bitmap, result)
                } catch (e: Exception) {
                    // Handle errors, for example, show a toast
                    Toast.makeText(this@DetectionActivity, "Error detecting disease: ${e.message}", Toast.LENGTH_LONG).show()
                    tvImageStatus.text = "Analysis failed"
                }
            }
        } ?: run {
            // Fallback to placeholder if no image
            val diseaseResult = getPlaceholderDiseaseResult()
            updateDiseaseInfo(diseaseResult)
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
        
        // Add to history if we have a valid bitmap
        currentBitmap?.let { bitmap ->
            // Create a copy of the bitmap to avoid recycling issues
            val bitmapCopy = bitmap.copy(bitmap.config, true)
            val historyItem = DetectionHistory(
                image = bitmapCopy,
                result = diseaseResult
            )
            detectionHistory.add(historyItem)
        }
    }

    /**
     * Add current detection to history
     */
    private fun addToHistory(bitmap: Bitmap, result: MLModelHelper.DetectionResult) {
        val copyBitmap = bitmap.copy(bitmap.config, true)
        detectionHistory.add(DetectionHistory(image = copyBitmap, result = result))
        
        // Update the history adapter if it's initialized
        if (::historyAdapter.isInitialized) {
            historyAdapter.updateItems(detectionHistory)
        }
    }

    /**
     * Load a history item into the main view
     */
    private fun loadHistoryItem(historyItem: DetectionHistory) {
        // Update the main image view
        ivPlantImage.setImageBitmap(historyItem.image)
        
        // Update current bitmap reference
        currentBitmap = historyItem.image
        
        // Update the UI with the history item's detection result
        updateDiseaseInfo(historyItem.result)
        
        // Close the history dialog if it's open
        // Note: This assumes the dialog is the last shown dialog
        // In a production app, you might want to keep a reference to the dialog
        try {
            (supportFragmentManager.findFragmentByTag("history_dialog") as? android.app.Dialog)?.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Show history dialog with previous detections
     */
    private fun showHistoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_history, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rv_history)
        val btnClearHistory = dialogView.findViewById<MaterialButton>(R.id.btn_clear_history)
        
        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = historyAdapter
        
        // Update adapter with current history
        historyAdapter.updateItems(detectionHistory)
        
        // Set up clear history button
        btnClearHistory.setOnClickListener {
            detectionHistory.clear()
            historyAdapter.updateItems(emptyList())
        }
        
        // Show dialog
        AlertDialog.Builder(this)
            .setTitle("Detection History")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }
     * Convert bitmap to URI for sharing
     */
    private fun getImageUri(bitmap: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            contentResolver,
            bitmap,
            "PlantDisease_${System.currentTimeMillis()}",
            null
        )
        return path?.let { Uri.parse(it) }
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