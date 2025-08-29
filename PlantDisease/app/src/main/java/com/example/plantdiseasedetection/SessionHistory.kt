package com.example.plantdiseasedetection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Session-scoped in-memory history for scans/uploads.
 * This data is cleared when the app process is killed.
 */
object SessionHistory {

    enum class SourceType { CAMERA, GALLERY }

    data class Entry(
        val timestampMillis: Long,
        val sourceType: SourceType,
        val imageBytes: ByteArray?,
        val diseaseName: String,
        val confidence: Float?,
        val cause: String,
        val prevention: String
    )

    private val entries: MutableList<Entry> = mutableListOf()

    fun add(
        bitmap: Bitmap?,
        sourceType: SourceType,
        diseaseName: String,
        confidence: Float?,
        cause: String,
        prevention: String
    ) {
        val bytes = bitmap?.let { compressToJpeg(it) }
        entries.add(
            Entry(
                timestampMillis = System.currentTimeMillis(),
                sourceType = sourceType,
                imageBytes = bytes,
                diseaseName = diseaseName,
                confidence = confidence,
                cause = cause,
                prevention = prevention
            )
        )
    }

    fun getAll(): List<Entry> = entries.asReversed() // newest first

    fun clear() { entries.clear() }

    private fun compressToJpeg(bitmap: Bitmap): ByteArray {
        val scaled = if (bitmap.width > 800 || bitmap.height > 800) {
            val ratio = minOf(800f / bitmap.width, 800f / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else bitmap
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

    fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun decode(bytes: ByteArray?): Bitmap? = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
}


