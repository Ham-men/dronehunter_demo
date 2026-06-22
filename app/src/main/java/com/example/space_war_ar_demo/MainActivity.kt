package com.example.space_war_ar_demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.space_war_ar_demo.utils.FullscreenManager
import com.example.space_war_ar_demo.utils.LanguageHelper

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var startButton: Button
    private lateinit var exitButton: Button
    private var isInitialized = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: start")
        try {
            super.onCreate(savedInstanceState)
            FullscreenManager.setupFullscreenMode(this)
            setContentView(R.layout.activity_main_ver2)
            initializeViews()
            setupButtons()
            FullscreenManager.hideSystemUI(this)
            isInitialized = true
            Log.d(TAG, "onCreate: completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate", e)
            Toast.makeText(this, "Критическая ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        startButton = findViewById(R.id.btnstart)
        exitButton = findViewById(R.id.btnexit)
    }

    private fun setupButtons() {
        startButton.setOnClickListener {
            Log.d(TAG, "start button clicked")
            try {
                val intent = Intent(this, GameActivityVer2::class.java)
                intent.putExtra("Level", 102)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting game", e)
                Toast.makeText(this, "Не удалось запустить игру", Toast.LENGTH_SHORT).show()
            }
        }

        exitButton.setOnClickListener {
            Log.d(TAG, "exit button clicked")
            finishAffinity()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isInitialized) {
            FullscreenManager.ensureFullscreenMode(this)
        }
    }
}
