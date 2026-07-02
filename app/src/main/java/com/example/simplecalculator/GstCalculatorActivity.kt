package com.riteshkatre.simplecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivityGstCalculatorBinding

class GstCalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGstCalculatorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityGstCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCalculate.setOnClickListener { calculate() }
        binding.btnCopy.setOnClickListener { copyResult() }
        binding.btnShare.setOnClickListener { shareResult() }
        binding.btnSave.setOnClickListener { saveResult() }

        listOf(binding.btn5, binding.btn12, binding.btn18).forEach { button ->
            button.setOnClickListener {
                binding.rateInput.setText(button.text.toString().replace("%", ""))
            }
        }
    }

    private fun calculate() {
        val amount = CalculatorMath.parseDouble(binding.amountInput.text?.toString().orEmpty())
        val rate = CalculatorMath.parseDouble(binding.rateInput.text?.toString().orEmpty())
        val gstAmount = CalculatorMath.percentOf(amount, rate)
        val total = amount + gstAmount
        binding.resultAmount.text = CalculatorMath.formatNumber(gstAmount)
        binding.resultTotal.text = CalculatorMath.formatNumber(total)
        binding.resultCard.visibility = android.view.View.VISIBLE
        binding.resultSummary.text = "${CalculatorMath.formatNumber(amount)} + ${CalculatorMath.formatNumber(rate)}% GST = ${CalculatorMath.formatNumber(total)}"
        HistoryStore.add(this, binding.resultSummary.text.toString(), "GST ${CalculatorMath.formatNumber(total)}")
    }

    private fun copyResult() {
        copyToClipboard(getString(R.string.app_name), binding.resultSummary.text.toString())
    }

    private fun shareResult() {
        sharePlainText(getString(R.string.app_name), binding.resultSummary.text.toString())
    }

    private fun saveResult() {
        val summary = binding.resultSummary.text?.toString().orEmpty()
        if (summary.isNotBlank()) {
            HistoryStore.add(this, summary, binding.resultTotal.text?.toString().orEmpty())
        }
    }
}
