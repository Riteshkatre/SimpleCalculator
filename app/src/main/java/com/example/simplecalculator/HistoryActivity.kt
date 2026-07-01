package com.riteshkatre.simplecalculator

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivityHistoryBinding
import com.riteshkatre.simplecalculator.databinding.ItemHistoryEntryBinding

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnClearAll.setOnClickListener { confirmClearAll() }
        AdManager.loadBanner(this, binding.bannerAdContainer)
        renderHistory()
    }

    private fun renderHistory() {
        binding.historyContainer.removeAllViews()
        val items = HistoryStore.load(this)
        binding.emptyHistory.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.btnClearAll.isEnabled = items.isNotEmpty()

        items.forEachIndexed { index, entry ->
            val row = ItemHistoryEntryBinding.inflate(LayoutInflater.from(this), binding.historyContainer, false)
            row.expressionText.text = entry.expression
            row.resultText.text = entry.result
            row.btnCopy.setOnClickListener {
                copyToClipboard(getString(R.string.app_name), entry.result)
            }
            row.btnShare.setOnClickListener {
                sharePlainText(getString(R.string.app_name), entry.shareText(getString(R.string.app_name)))
            }
            row.btnDelete.setOnClickListener {
                HistoryStore.deleteAt(this, index)
                renderHistory()
            }
            binding.historyContainer.addView(row.root)
        }
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle("Clear history")
            .setMessage("Remove all saved calculations?")
            .setPositiveButton("Clear") { _, _ ->
                HistoryStore.clear(this)
                renderHistory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
