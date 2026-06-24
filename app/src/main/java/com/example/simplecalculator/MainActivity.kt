package com.riteshkatre.simplecalculator

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivityMainBinding
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentInput = "0"
    private var leftOperand: Double? = null
    private var pendingOperator: String? = null
    private var justEvaluated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindThemeToggle()
        bindButtons()
        renderDisplay()
    }

    private fun bindThemeToggle() {
        updateThemeIcon()
        binding.themeToggle.setOnClickListener {
            val current = AppTheme.getThemeMode(this)
            val next = if (current == AppTheme.MODE_DARK) AppTheme.MODE_LIGHT else AppTheme.MODE_DARK
            AppTheme.setThemeMode(this, next)
        }
    }

    private fun updateThemeIcon() {
        val isDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        binding.themeToggle.contentDescription = if (isDark) {
            getString(R.string.switch_to_light_theme)
        } else {
            getString(R.string.switch_to_dark_theme)
        }
    }

    private fun bindButtons() {
        binding.btn0.setOnClickListener { onDigitPressed("0") }
        binding.btn1.setOnClickListener { onDigitPressed("1") }
        binding.btn2.setOnClickListener { onDigitPressed("2") }
        binding.btn3.setOnClickListener { onDigitPressed("3") }
        binding.btn4.setOnClickListener { onDigitPressed("4") }
        binding.btn5.setOnClickListener { onDigitPressed("5") }
        binding.btn6.setOnClickListener { onDigitPressed("6") }
        binding.btn7.setOnClickListener { onDigitPressed("7") }
        binding.btn8.setOnClickListener { onDigitPressed("8") }
        binding.btn9.setOnClickListener { onDigitPressed("9") }
        binding.btnDoubleZero.setOnClickListener { onDigitPressed("00") }
        binding.btnDecimal.setOnClickListener { onDecimalPressed() }
        binding.btnClear.setOnClickListener { clearAll() }
        binding.btnBackspace.setOnClickListener { backspacePressed() }
        binding.btnPercent.setOnClickListener { percentPressed() }

        binding.btnDivide.setOnClickListener { onOperatorPressed("/") }
        binding.btnMultiply.setOnClickListener { onOperatorPressed("×") }
        binding.btnMinus.setOnClickListener { onOperatorPressed("−") }
        binding.btnPlus.setOnClickListener { onOperatorPressed("+") }
        binding.btnEquals.setOnClickListener { onEqualsPressed() }
    }

    private fun onDigitPressed(digit: String) {
        if (justEvaluated && pendingOperator == null) {
            clearAll()
        }

        currentInput = when {
            currentInput == "0" && digit == "00" -> "0"
            currentInput == "0" -> digit
            currentInput == "Error" -> digit
            else -> currentInput + digit
        }

        justEvaluated = false
        renderDisplay()
    }

    private fun onDecimalPressed() {
        if (justEvaluated && pendingOperator == null) {
            clearAll()
        }

        currentInput = when {
            currentInput == "Error" -> "0."
            currentInput.contains(".") -> currentInput
            currentInput == "0" -> "0."
            else -> "$currentInput."
        }

        justEvaluated = false
        renderDisplay()
    }

    private fun percentPressed() {
        val value = currentInputValue() / 100.0
        currentInput = formatNumber(value)
        justEvaluated = false
        renderDisplay()
    }

    private fun backspacePressed() {
        if (currentInput == "Error" || currentInput == "0") {
            currentInput = "0"
            renderDisplay()
            return
        }

        currentInput = when {
            currentInput.length <= 1 -> "0"
            currentInput.length == 2 && currentInput.startsWith("-") -> "0"
            else -> currentInput.dropLast(1)
        }

        if (currentInput == "-" || currentInput.isBlank()) {
            currentInput = "0"
        }

        renderDisplay()
    }

    private fun onOperatorPressed(operator: String) {
        val current = currentInputValue()

        if (leftOperand == null) {
            leftOperand = current
        } else if (pendingOperator != null && !justEvaluated) {
            leftOperand = calculate(leftOperand!!, current, pendingOperator!!)
        }

        pendingOperator = operator
        currentInput = "0"
        justEvaluated = false
        renderDisplay()
    }

    private fun onEqualsPressed() {
        val operator = pendingOperator ?: return
        val left = leftOperand ?: currentInputValue()
        val right = currentInputValue()

        currentInput = formatNumber(calculate(left, right, operator))
        leftOperand = null
        pendingOperator = null
        justEvaluated = true
        renderDisplay()
    }

    private fun clearAll() {
        currentInput = "0"
        leftOperand = null
        pendingOperator = null
        justEvaluated = false
        renderDisplay()
    }

    private fun currentInputValue(): Double {
        return normalizeNumericString(currentInput).toDoubleOrNull() ?: 0.0
    }

    private fun calculate(left: Double, right: Double, operator: String): Double {
        return when (operator) {
            "+" -> left + right
            "−" -> left - right
            "×" -> left * right
            "/" -> if (right == 0.0) Double.NaN else left / right
            else -> right
        }
    }

    private fun normalizeNumericString(value: String): String {
        return when {
            value.isBlank() -> "0"
            value == "-" -> "0"
            value.endsWith(".") -> value.dropLast(1)
            else -> value
        }
    }

    private fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) {
            return "Error"
        }

        val normalized = if (abs(value) < 0.0000001) 0.0 else value
        val rounded = BigDecimal.valueOf(normalized)
            .setScale(10, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

        val parts = rounded.split(".")
        val integerPart = parts[0].replace("-", "")
        val formattedInteger = DecimalFormat("#,###", DecimalFormatSymbols(Locale.getDefault())).format(integerPart.toLong())
        return if (rounded.startsWith("-")) {
            "-$formattedInteger" + parts.getOrNull(1)?.let { ".$it" }.orEmpty()
        } else {
            formattedInteger + parts.getOrNull(1)?.let { ".$it" }.orEmpty()
        }
    }

    private fun buildExpressionText(): String {
        return when {
            leftOperand != null && pendingOperator != null -> {
                val left = formatNumber(leftOperand!!)
                if (currentInput == "0" && !justEvaluated) {
                    "$left$pendingOperator"
                } else {
                    "$left$pendingOperator$currentInput"
                }
            }
            else -> currentInput
        }
    }

    private fun buildPreviewText(): String {
        return when {
            leftOperand != null && pendingOperator != null && currentInput != "0" ->
                formatNumber(calculate(leftOperand!!, currentInputValue(), pendingOperator!!))
            else -> ""
        }
    }

    private fun renderDisplay() {
        binding.expressionText.text = buildExpressionText()
        binding.previewText.text = buildPreviewText()
        binding.previewText.alpha = if (binding.previewText.text.isNullOrEmpty()) 0f else 1f

        val showCursor = pendingOperator != null || currentInput != "0" || justEvaluated
        binding.expressionCursor.visibility = if (showCursor) View.VISIBLE else View.INVISIBLE
    }
}
