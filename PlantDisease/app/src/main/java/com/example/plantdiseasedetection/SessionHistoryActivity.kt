package com.example.plantdiseasedetection

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class SessionHistoryActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnClearSession: MaterialButton
    private lateinit var tvEmptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_history)

        sessionManager = SessionManager(this)
        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadSessionData()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recycler_view_session)
        btnClearSession = findViewById(R.id.btn_clear_session)
        tvEmptyState = findViewById(R.id.tv_empty_state)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        btnClearSession.setOnClickListener {
            sessionManager.clearSession()
            loadSessionData()
        }
    }

    private fun loadSessionData() {
        val sessionItems = sessionManager.getAllSessionItems()
        
        if (sessionItems.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
            btnClearSession.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            btnClearSession.visibility = View.VISIBLE
            
            val adapter = SessionAdapter(sessionItems, sessionManager)
            recyclerView.adapter = adapter
        }
    }

    class SessionAdapter(
        private val sessionItems: List<SessionManager.SessionItem>,
        private val sessionManager: SessionManager
    ) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

        class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cardView: MaterialCardView = itemView.findViewById(R.id.card_session_item)
            val ivImage: ImageView = itemView.findViewById(R.id.iv_session_image)
            val tvDiseaseName: TextView = itemView.findViewById(R.id.tv_session_disease_name)
            val tvConfidence: TextView = itemView.findViewById(R.id.tv_session_confidence)
            val tvTimestamp: TextView = itemView.findViewById(R.id.tv_session_timestamp)
            val tvImageSource: TextView = itemView.findViewById(R.id.tv_session_image_source)
            val tvCause: TextView = itemView.findViewById(R.id.tv_session_cause)
            val tvPrevention: TextView = itemView.findViewById(R.id.tv_session_prevention)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_session_history, parent, false)
            return SessionViewHolder(view)
        }

        override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
            val item = sessionItems[position]
            
            // Load image
            val bitmap = sessionManager.base64ToBitmap(item.imageBase64)
            bitmap?.let {
                holder.ivImage.setImageBitmap(it)
            }
            
            // Set text fields
            holder.tvDiseaseName.text = item.detectionResult.diseaseName
            holder.tvConfidence.text = "Confidence: ${(item.detectionResult.confidence * 100).toInt()}%"
            holder.tvTimestamp.text = sessionManager.getFormattedTimestamp(item.timestamp)
            holder.tvImageSource.text = sessionManager.getImageSourceDisplayText(item.imageSource)
            holder.tvCause.text = item.detectionResult.cause
            holder.tvPrevention.text = item.detectionResult.prevention
            
            // Set confidence color based on level
            val confidenceColor = when {
                item.detectionResult.confidence > 0.8f -> android.R.color.holo_green_dark
                item.detectionResult.confidence > 0.6f -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_red_dark
            }
            holder.tvConfidence.setTextColor(holder.itemView.context.getColor(confidenceColor))
        }

        override fun getItemCount(): Int = sessionItems.size
    }
}
