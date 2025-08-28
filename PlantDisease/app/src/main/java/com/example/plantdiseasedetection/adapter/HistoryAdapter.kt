package com.example.plantdiseasedetection.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plantdiseasedetection.R
import com.example.plantdiseasedetection.model.DetectionHistory
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HistoryAdapter(
    private val onItemClick: (DetectionHistory) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val items = mutableListOf<DetectionHistory>()

    fun updateItems(newItems: List<DetectionHistory>) {
        items.clear()
        // Sort by timestamp in descending order (newest first)
        items.addAll(newItems.sortedByDescending { it.timestamp })
        notifyDataSetChanged()
    }

    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.iv_history_thumbnail)
        private val diseaseName: TextView = itemView.findViewById(R.id.tv_disease_name)
        private val confidence: TextView = itemView.findViewById(R.id.tv_confidence)
        private val timestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        
        init {
            // Set minimum height for better touch targets
            itemView.minimumHeight = itemView.resources.getDimensionPixelSize(R.dimen.history_item_min_height)
        }

        fun bind(item: DetectionHistory) {
            try {
                // Set thumbnail
                thumbnail.setImageBitmap(item.image)

                // Set disease name
                diseaseName.text = item.result.diseaseName

                // Set confidence
                val confidencePercent = (item.result.confidence * 100).toInt()
                confidence.text = "$confidencePercent% confidence"

                // Set timestamp
                val timeAgo = getTimeAgo(item.timestamp)
                timestamp.text = timeAgo

                // Set click listener
                itemView.setOnClickListener { onItemClick(item) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "$hours ${if (hours == 1L) "hour" else "hours"} ago"
                }
                else -> {
                    val date = Date(timestamp)
                    val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    format.format(date)
                }
            }
        }
    }
}
