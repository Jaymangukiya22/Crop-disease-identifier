package com.example.plantdiseasedetection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private val entries: List<SessionHistory.Entry>) : RecyclerView.Adapter<HistoryAdapter.Holder>() {

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.iv_history_image)
        val title: TextView = itemView.findViewById(R.id.tv_history_title)
        val details: TextView = itemView.findViewById(R.id.tv_history_details)
        val meta: TextView = itemView.findViewById(R.id.tv_history_meta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = entries.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = entries[position]
        holder.image.setImageBitmap(SessionHistory.decode(entry.imageBytes))
        val confidenceText = entry.confidence?.let { "${(it * 100).toInt()}%" } ?: "-"
        holder.title.text = "${entry.diseaseName}  (${confidenceText})"
        holder.details.text = "Cause: ${entry.cause}\nPrevention: ${entry.prevention}"
        holder.meta.text = "${SessionHistory.formatTimestamp(entry.timestampMillis)}  •  ${entry.sourceType.name}"
    }
}


