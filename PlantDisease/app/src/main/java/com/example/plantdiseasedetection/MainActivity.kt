package com.example.plantdiseasedetection

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * MainActivity - Welcome Page
 * 
 * This activity serves as the welcome screen for the ML-based Plant Disease Detection app.
 * It provides a clean, professional interface with a "Let's Get Started" button that
 * navigates to the DetectionActivity.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        setupUI()
    }

    /**
     * Sets up the user interface components and their click listeners
     */
    private fun setupUI() {
        val btnGetStarted = findViewById<MaterialButton>(R.id.btn_get_started)
        
        btnGetStarted.setOnClickListener {
            // Navigate to DetectionActivity
            val intent = Intent(this, DetectionActivity::class.java)
            startActivity(intent)
        }
    }
}