package com.example.plantdiseasedetection

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * MainActivity - Welcome Page
 * 
 * This activity serves as the welcome screen for the ML-based Plant Disease Detection app.
 * It provides a clean, professional interface with buttons to navigate to detection or view session history.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)
        
        // Initialize UI components
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        // Update session history visibility when returning to this activity
        updateSessionHistoryVisibility()
    }

    /**
     * Sets up the user interface components and their click listeners
     */
    private fun setupUI() {
        val btnGetStarted = findViewById<MaterialButton>(R.id.btn_get_started)
        val btnViewHistory = findViewById<MaterialButton>(R.id.btn_view_history)
        val cardSessionHistory = findViewById<MaterialCardView>(R.id.card_session_history)
        
        btnGetStarted.setOnClickListener {
            // Navigate to DetectionActivity
            val intent = Intent(this, DetectionActivity::class.java)
            startActivity(intent)
        }
        
        btnViewHistory.setOnClickListener {
            // Navigate to SessionHistoryActivity
            val intent = Intent(this, SessionHistoryActivity::class.java)
            startActivity(intent)
        }
        
        // Initial visibility check
        updateSessionHistoryVisibility()
    }
    
    /**
     * Update the visibility of session history elements based on whether session data exists
     */
    private fun updateSessionHistoryVisibility() {
        val cardSessionHistory = findViewById<MaterialCardView>(R.id.card_session_history)
        val btnViewHistory = findViewById<MaterialButton>(R.id.btn_view_history)
        
        if (sessionManager.hasSessionData()) {
            cardSessionHistory.visibility = android.view.View.VISIBLE
            btnViewHistory.visibility = android.view.View.VISIBLE
        } else {
            cardSessionHistory.visibility = android.view.View.GONE
            btnViewHistory.visibility = android.view.View.GONE
        }
    }
}