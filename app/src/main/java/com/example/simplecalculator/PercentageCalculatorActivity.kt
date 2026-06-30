package com.riteshkatre.simplecalculator

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivityPercentageCalculatorBinding

class PercentageCalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPercentageCalculatorBinding
    private var mode = Mode.OF

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityPercentageCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCalculate.setOnClickListener { calculate() }
        binding.btnCopy.setOnClickListener { copyToClipboard(getString(R.string.app_name), binding.resultSummary.text.toString()) }
        binding.btnShare.setOnClickListener { sharePlainText(getString(R.string.app_name), binding.resultSummary.text.toString()) }
        binding.btnSave.setOnClickListener { saveResult() }

        binding.btnModeOf.setOnClickListener { setMode(Mode.OF) }
        binding.btnModeAdd.setOnClickListener { setMode(Mode.ADD) }
        binding.btnModeSubtract.setOnClickListener { setMode(Mode.SUBTRACT) }
        updateModeButtons()
    }

    private fun setMode(newMode: Mode) {
        mode = newMode
        updateModeButtons()
    }

    private fun updateModeButtons() {
        val activeTint = ColorStateList.valueOf(getColor(R.color.operator_button_background))
        val inactiveTint = ColorStateList.valueOf(getColor(R.color.screen_background))
        val activeText = getColor(R.color.operator_button_text)
        val inactiveText = getColor(R.color.display_text)

        val states = listOf(
            binding.btnModeOf to (mode == Mode.OF),
            binding.btnModeAdd to (mode == Mode.ADD),
            binding.btnModeSubtract to (mode == Mode.SUBTRACT),
        )
        states.forEach { (button, selected) ->
            button.backgroundTintList = if (selected) activeTint else inactiveTint
            button.setTextColor(if (selected) activeText else inactiveText)
        }
    }

    private fun calculate() {
        val value = CalculatorMath.parseDouble(binding.valueInput.text?.toString().orEmpty())
        val percent = CalculatorMath.parseDouble(binding.percentInput.text?.toString().orEmpty())
        val result = when (mode) {
            Mode.OF -> CalculatorMath.percentOf(value, percent)
            Mode.ADD -> CalculatorMath.percentageAdjustment(value, percent, true)
            Mode.SUBTRACT -> CalculatorMath.percentageAdjustment(value, percent, false)
        }
        val label = when (mode) {
            Mode.OF -> "${CalculatorMath.formatNumber(percent)}% of ${CalculatorMath.formatNumber(value)}"
            Mode.ADD -> "${CalculatorMath.formatNumber(value)} + ${CalculatorMath.formatNumber(percent)}%"
            Mode.SUBTRACT -> "${CalculatorMath.formatNumber(value)} - ${CalculatorMath.formatNumber(percent)}%"
        }
        binding.resultSummary.text = "$label = ${CalculatorMath.formatNumber(result)}"
        binding.resultCard.visibility = android.view.View.VISIBLE
        HistoryStore.add(this, binding.resultSummary.text.toString(), CalculatorMath.formatNumber(result))
    }

    private fun saveResult() {
        val summary = binding.resultSummary.text?.toString().orEmpty()
        if (summary.isNotBlank()) {
            HistoryStore.add(this, summary, summary.substringAfter("=").trim())
        }
    }

    private enum class Mode { OF, ADD, SUBTRACT }
}
