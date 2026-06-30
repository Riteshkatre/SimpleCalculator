package com.riteshkatre.simplecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivityEmiCalculatorBinding
import kotlin.math.pow

class EmiCalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmiCalculatorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityEmiCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCalculate.setOnClickListener { calculate() }
        binding.btnCopy.setOnClickListener { copyToClipboard(getString(R.string.app_name), binding.resultSummary.text.toString()) }
        binding.btnShare.setOnClickListener { sharePlainText(getString(R.string.app_name), binding.resultSummary.text.toString()) }
        binding.btnSave.setOnClickListener { saveResult() }
    }

    private fun calculate() {
        val principal = CalculatorMath.parseDouble(binding.loanAmountInput.text?.toString().orEmpty())
        val annualRate = CalculatorMath.parseDouble(binding.interestRateInput.text?.toString().orEmpty())
        val months = CalculatorMath.parseDouble(binding.monthsInput.text?.toString().orEmpty())
        if (principal <= 0 || annualRate < 0 || months <= 0) {
            binding.resultSummary.text = "Enter valid loan, rate, and months."
            binding.resultCard.visibility = android.view.View.VISIBLE
            return
        }

        val monthlyRate = annualRate / 12.0 / 100.0
        val emi = if (monthlyRate == 0.0) {
            principal / months
        } else {
            val factor = (1 + monthlyRate).pow(months)
            principal * monthlyRate * factor / (factor - 1)
        }
        val totalPayment = emi * months
        val totalInterest = totalPayment - principal

        binding.resultEmi.text = CalculatorMath.formatNumber(emi)
        binding.resultInterest.text = CalculatorMath.formatNumber(totalInterest)
        binding.resultPayment.text = CalculatorMath.formatNumber(totalPayment)
        binding.resultSummary.text = "EMI for ${CalculatorMath.formatNumber(principal)} at ${CalculatorMath.formatNumber(annualRate)}% for ${CalculatorMath.formatNumber(months)} months"
        binding.resultCard.visibility = android.view.View.VISIBLE
        HistoryStore.add(this, binding.resultSummary.text.toString(), "EMI ${CalculatorMath.formatNumber(emi)}")
    }

    private fun saveResult() {
        val summary = binding.resultSummary.text?.toString().orEmpty()
        if (summary.isNotBlank()) {
            HistoryStore.add(this, summary, binding.resultEmi.text?.toString().orEmpty())
        }
    }
}
