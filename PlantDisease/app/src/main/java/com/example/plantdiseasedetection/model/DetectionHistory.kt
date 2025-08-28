package com.example.plantdiseasedetection.model

import android.graphics.Bitmap
import android.os.Parcelable
import com.example.plantdiseasedetection.MLModelHelper
import kotlinx.parcelize.Parcelize
import java.io.ByteArrayOutputStream
import java.util.*

/**
 * Data class representing a single detection history item
 */
@Parcelize
data class DetectionHistory(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val image: Bitmap,
    val result: MLModelHelper.DetectionResult
) : Parcelable {
    
    /**
     * Convert bitmap to Base64 string for parceling
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.getEncoder().encodeToString(byteArray)
    }
    
    /**
     * Get a formatted date string from timestamp
     */
    fun getFormattedDate(): String {
        val date = Date(timestamp)
        val sdf = java.text.SimpleDateFormat("MMM d, yyyy hh:mm a", Locale.getDefault())
        return sdf.format(date)
    }
