package com.example.plantdiseasedetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * SessionManager - Manages session data for captured images and detection results
 * 
 * This class handles storing and retrieving session data including images and their
 * detection results. Data is stored in memory and persists only during the app session.
 */
class SessionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SessionManager"
        private const val IMAGE_DIR = "session_images"
    }
    
    // In-memory storage for session data
    private val sessionData = mutableListOf<SessionItem>()
    
    /**
     * Data class representing a session item with image and detection results
     */
    data class SessionItem(
        val id: String,
        val imagePath: String,
        val imageBase64: String,
        val timestamp: Long,
        val detectionResult: MLModelHelper.DetectionResult,
        val imageSource: String // "camera" or "gallery"
    )
    
    /**
     * Add a new item to the session
     */
    fun addSessionItem(
        bitmap: Bitmap,
        detectionResult: MLModelHelper.DetectionResult,
        imageSource: String
    ): String {
        val id = generateId()
        val timestamp = System.currentTimeMillis()
        
        // Save image to internal storage
        val imagePath = saveImageToStorage(bitmap, id)
        
        // Convert bitmap to base64 for in-memory storage
        val imageBase64 = bitmapToBase64(bitmap)
        
        val sessionItem = SessionItem(
            id = id,
            imagePath = imagePath,
            imageBase64 = imageBase64,
            timestamp = timestamp,
            detectionResult = detectionResult,
            imageSource = imageSource
        )
        
        sessionData.add(sessionItem)
        Log.d(TAG, "Added session item: $id, total items: ${sessionData.size}")
        
        return id
    }
    
    /**
     * Get all session items
     */
    fun getAllSessionItems(): List<SessionItem> {
        return sessionData.toList()
    }
    
    /**
     * Get session item by ID
     */
    fun getSessionItem(id: String): SessionItem? {
        return sessionData.find { it.id == id }
    }
    
    /**
     * Check if session has any items
     */
    fun hasSessionData(): Boolean {
        return sessionData.isNotEmpty()
    }
    
    /**
     * Get session item count
     */
    fun getSessionItemCount(): Int {
        return sessionData.size
    }
    
    /**
     * Clear all session data
     */
    fun clearSession() {
        // Delete saved image files
        sessionData.forEach { item ->
            try {
                val file = File(item.imagePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting image file: ${e.message}")
            }
        }
        
        sessionData.clear()
        Log.d(TAG, "Session cleared")
    }
    
    /**
     * Generate unique ID for session items
     */
    private fun generateId(): String {
        return "session_${System.currentTimeMillis()}_${Random().nextInt(1000)}"
    }
    
    /**
     * Save bitmap to internal storage
     */
    private fun saveImageToStorage(bitmap: Bitmap, id: String): String {
        val imageDir = File(context.filesDir, IMAGE_DIR)
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }
        
        val imageFile = File(imageDir, "${id}.jpg")
        
        try {
            val outputStream = imageFile.outputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()
            
            Log.d(TAG, "Image saved to: ${imageFile.absolutePath}")
            return imageFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image: ${e.message}")
            throw e
        }
    }
    
    /**
     * Convert bitmap to base64 string
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    /**
     * Convert base64 string back to bitmap
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting base64 to bitmap: ${e.message}")
            null
        }
    }
    
    /**
     * Get formatted timestamp
     */
    fun getFormattedTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * Get image source display text
     */
    fun getImageSourceDisplayText(imageSource: String): String {
        return when (imageSource) {
            "camera" -> "📷 Camera"
            "gallery" -> "🖼️ Gallery"
            else -> "📱 Unknown"
        }
    }
}
