package com.riteshkatre.simplecalculator

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val handler = Handler(Looper.getMainLooper())
        var navigated = false
        val navigate = Runnable {
            if (navigated) return@Runnable
            navigated = true
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val appOpenShown = AdManager.showAppOpen(this) {
            handler.removeCallbacks(navigate)
            navigate.run()
        }
        if (!appOpenShown) {
            handler.postDelayed(navigate, 1200)
        }
    }
}
