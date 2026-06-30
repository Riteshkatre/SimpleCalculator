package com.riteshkatre.simplecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.riteshkatre.simplecalculator.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.themeGroup.check(
            when (AppTheme.getThemeMode(this)) {
                AppTheme.MODE_LIGHT -> R.id.themeLight
                AppTheme.MODE_DARK -> R.id.themeDark
                else -> R.id.themeSystem
            }
        )
        binding.themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.themeLight -> AppTheme.MODE_LIGHT
                R.id.themeDark -> AppTheme.MODE_DARK
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppTheme.setThemeMode(this, mode)
        }
    }
}
