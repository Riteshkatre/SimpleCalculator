package com.example.simplecalculator

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.example.simplecalculator.databinding.ActivityMainBinding
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var expressionText = "0"
    private var cursorPosition = 1
    private var leftOperand: Double? = null
    private var pendingOperator: String? = null
    private var userIsTyping = false
    private var justEvaluated = false
    private var suppressTextWatcher = false

    private val expressionWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable?) {
            if (suppressTextWatcher) return

            val text = s?.toString().orEmpty()
            expressionText = if (text.isBlank()) "0" else text
            cursorPosition = binding.expressionText.selectionStart.coerceAtLeast(0)
            userIsTyping = true
            justEvaluated = false
            updatePreview()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupExpressionField()
        bindThemeToggle()
        bindButtons()
        renderDisplay()
    }

    private fun setupExpressionField() {
        binding.expressionText.showSoftInputOnFocus = false
        binding.expressionText.setOnClickListener {
            binding.expressionText.requestFocus()
            hideKeyboard(it)
        }
        binding.expressionText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                hideKeyboard(binding.expressionText)
            }
        }
        binding.expressionText.addTextChangedListener(expressionWatcher)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
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
        binding.btn0.setOnClickListener { insertAtCursor("0") }
        binding.btn1.setOnClickListener { insertAtCursor("1") }
        binding.btn2.setOnClickListener { insertAtCursor("2") }
        binding.btn3.setOnClickListener { insertAtCursor("3") }
        binding.btn4.setOnClickListener { insertAtCursor("4") }
        binding.btn5.setOnClickListener { insertAtCursor("5") }
        binding.btn6.setOnClickListener { insertAtCursor("6") }
        binding.btn7.setOnClickListener { insertAtCursor("7") }
        binding.btn8.setOnClickListener { insertAtCursor("8") }
        binding.btn9.setOnClickListener { insertAtCursor("9") }
        binding.btnBackspace.setOnClickListener { insertAtCursor("00") }
        binding.btnDecimal.setOnClickListener { insertAtCursor(".") }
        binding.btnClear.setOnClickListener { clearAll() }
        binding.btnToggleSign.setOnClickListener { backspacePressed() }
        binding.btnPercent.setOnClickListener { percentPressed() }

        binding.btnDivide.setOnClickListener { onOperatorPressed("/") }
        binding.btnMultiply.setOnClickListener { onOperatorPressed("×") }
        binding.btnMinus.setOnClickListener { onOperatorPressed("−") }
        binding.btnPlus.setOnClickListener { onOperatorPressed("+") }
        binding.btnEquals.setOnClickListener { onEqualsPressed() }
    }

    private fun insertAtCursor(text: String) {
        if (justEvaluated && pendingOperator == null) {
            clearAll()
        }

        val selectionStart = binding.expressionText.selectionStart.takeIf { it >= 0 } ?: cursorPosition
        val selectionEnd = binding.expressionText.selectionEnd.takeIf { it >= 0 } ?: cursorPosition
        var start = minOf(selectionStart, selectionEnd)
        var end = maxOf(selectionStart, selectionEnd)
        val current = normalizeNumericString(expressionText)

        val replacement = if (current == "0" && start == 0 && end == current.length && text != "00") {
            text
        } else if (current == "0" && text == "00") {
            "0"
        } else {
            text
        }

        val newText = buildString {
            append(current.substring(0, start))
            append(replacement)
            append(current.substring(end))
        }

        setExpressionText(if (newText.isBlank()) "0" else newText)
        cursorPosition = (start + replacement.length).coerceAtMost(expressionText.length)
        renderDisplay()
    }

    private fun onDigitPressed(digit: String) {
        insertAtCursor(digit)
    }

    private fun onDecimalPressed() {
        val current = normalizeNumericString(expressionText)
        if (current.contains(".")) return
        insertAtCursor(".")
    }

    private fun percentPressed() {
        val value = currentValueOrZero() / 100.0
        setExpressionText(formatNumber(value))
        cursorPosition = expressionText.length
        renderDisplay()
    }

    private fun backspacePressed() {
        val current = normalizeNumericString(expressionText)
        val selectionStart = binding.expressionText.selectionStart.takeIf { it >= 0 } ?: cursorPosition
        val selectionEnd = binding.expressionText.selectionEnd.takeIf { it >= 0 } ?: cursorPosition
        val start = minOf(selectionStart, selectionEnd)
        val end = maxOf(selectionStart, selectionEnd)

        val updated = when {
            start != end -> buildString {
                append(current.substring(0, start))
                append(current.substring(end))
            }
            start > 0 -> buildString {
                append(current.substring(0, start - 1))
                append(current.substring(end))
            }
            else -> current
        }

        setExpressionText(if (updated.isBlank()) "0" else updated)
        cursorPosition = (if (start != end) start else (start - 1).coerceAtLeast(0)).coerceAtMost(expressionText.length)
        renderDisplay()
    }

    private fun onOperatorPressed(operator: String) {
        val current = currentValueOrZero()

        if (leftOperand == null) {
            leftOperand = current
        } else if (pendingOperator != null && userIsTyping) {
            leftOperand = calculate(leftOperand!!, current, pendingOperator!!)
        }

        pendingOperator = operator
        setExpressionText(formatNumber(leftOperand ?: current))
        cursorPosition = expressionText.length
        userIsTyping = false
        justEvaluated = false
        renderDisplay()
    }

    private fun onEqualsPressed() {
        val operator = pendingOperator ?: return
        val left = leftOperand ?: currentValueOrZero()
        val right = if (userIsTyping) currentValueOrZero() else left

        setExpressionText(formatNumber(calculate(left, right, operator)))
        cursorPosition = expressionText.length
        leftOperand = null
        pendingOperator = null
        userIsTyping = false
        justEvaluated = true
        renderDisplay()
    }

    private fun clearAll() {
        leftOperand = null
        pendingOperator = null
        userIsTyping = false
        justEvaluated = false
        setExpressionText("0")
        cursorPosition = 1
        renderDisplay()
    }

    private fun setExpressionText(value: String) {
        suppressTextWatcher = true
        expressionText = if (value.isBlank()) "0" else value
        binding.expressionText.setText(expressionText)
        suppressTextWatcher = false
        binding.expressionText.setSelection(cursorPosition.coerceIn(0, binding.expressionText.text.length))
    }

    private fun currentValueOrZero(): Double {
        return normalizeNumericString(expressionText).toDoubleOrNull() ?: 0.0
    }

    private fun normalizeNumericString(value: String): String {
        return when {
            value.isBlank() -> "0"
            value == "-" -> "0"
            value.endsWith(".") -> value.dropLast(1)
            else -> value
        }
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

    private fun renderDisplay() {
        suppressTextWatcher = true
        binding.expressionText.setText(expressionText)
        binding.expressionText.setSelection(cursorPosition.coerceIn(0, binding.expressionText.text.length))
        suppressTextWatcher = false
        updatePreview()
    }

    private fun updatePreview() {
        val previewText = buildPreviewText()
        binding.previewText.text = previewText
        binding.previewText.alpha = if (previewText.isEmpty()) 0f else 1f
    }

    private fun buildPreviewText(): String {
        return when {
            leftOperand != null && pendingOperator != null && userIsTyping ->
                formatNumber(calculate(leftOperand!!, currentValueOrZero(), pendingOperator!!))
            else -> ""
        }
    }
}
