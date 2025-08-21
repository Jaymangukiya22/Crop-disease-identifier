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
    private lateinit var ivPlantImage: ImageView
    private lateinit var tvImageStatus: TextView
    private lateinit var tvDiseaseName: TextView
    private lateinit var tvCause: TextView
    private lateinit var tvPrevention: TextView

    // Image handling
    private var currentImagePath: String? = null
    private var currentImageUri: Uri? = null
    private var currentBitmap: Bitmap? = null

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

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        btnCaptureImage = findViewById(R.id.btn_capture_image)
        btnUploadImage = findViewById(R.id.btn_upload_image)
        ivPlantImage = findViewById(R.id.iv_plant_image)
        tvImageStatus = findViewById(R.id.tv_image_status)
        tvDiseaseName = findViewById(R.id.tv_disease_name)
        tvCause = findViewById(R.id.tv_cause)
        tvPrevention = findViewById(R.id.tv_prevention)
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
            // Perform disease detection using ML model
            mlModelHelper.detectDisease(bitmap, object : MLModelHelper.DiseaseDetectionCallback {
                override fun onSuccess(result: MLModelHelper.DetectionResult) {
                    runOnUiThread {
                        updateDiseaseInfo(result)
                        tvImageStatus.text = "Analysis complete (${(result.confidence * 100).toInt()}% confidence)"
                    }
                }
                
                override fun onError(error: String) {
                    runOnUiThread {
                        Toast.makeText(this@DetectionActivity, error, Toast.LENGTH_LONG).show()
                        tvImageStatus.text = "Analysis failed"
                    }
                }
            })
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
                "Moderate"
            ),
            MLModelHelper.DetectionResult(
                "Powdery Mildew",
                0.92f,
                "Fungal disease caused by Erysiphe cichoracearum",
                "Apply sulfur-based fungicide, increase plant spacing, improve ventilation",
                "High"
            ),
            MLModelHelper.DetectionResult(
                "Bacterial Spot",
                0.78f,
                "Bacterial infection caused by Xanthomonas campestris",
                "Remove infected plants, apply copper-based bactericide, avoid overhead watering",
                "Moderate"
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