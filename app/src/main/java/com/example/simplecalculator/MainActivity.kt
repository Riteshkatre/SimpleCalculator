package com.riteshkatre.simplecalculator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivityMainBinding
import kotlin.math.PI

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentInput = "0"
    private var leftOperand: Double? = null
    private var pendingOperator: String? = null
    private var justEvaluated = false
    private var scientificMode = false
    private var angleModeDegrees = true
    private var inverseMode = false
    private var lastShareExpression = ""
    private var lastShareResult = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindTopBar()
        bindButtons()
        setupBannerAd()
        updateScientificPanel()
        renderDisplay()
    }

    private fun setupBannerAd() {
        AdManager.loadBanner(this, binding.bannerAdContainer)
    }

    private fun bindTopBar() {
        binding.btnScientificMode.setOnClickListener {
            scientificMode = !scientificMode
            updateScientificPanel()
        }

        binding.btnMoreCalculators.setOnClickListener {
            startActivity(Intent(this, CalculatorsActivity::class.java))
        }

        binding.btnOverflowMenu.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menuInflater.inflate(R.menu.main_overflow_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_history -> {
                        startActivity(Intent(this, HistoryActivity::class.java))
                        true
                    }
                    R.id.menu_settings -> {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            popup.show()
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

        binding.btnDivide.setOnClickListener { onOperatorPressed("÷") }
        binding.btnMultiply.setOnClickListener { onOperatorPressed("×") }
        binding.btnMinus.setOnClickListener { onOperatorPressed("−") }
        binding.btnPlus.setOnClickListener { onOperatorPressed("+") }
        binding.btnEquals.setOnClickListener { onEqualsPressed() }

        binding.btnCopyResult.setOnClickListener { copyCurrentResult() }
        binding.btnShareResult.setOnClickListener { shareCurrentResult() }
        binding.btnSaveResult.setOnClickListener { saveCurrentResult() }

        binding.btnSin.setOnClickListener { applyScientific("sin") }
        binding.btnCos.setOnClickListener { applyScientific("cos") }
        binding.btnTan.setOnClickListener { applyScientific("tan") }
        binding.btnLog.setOnClickListener { applyScientific("log") }
        binding.btnLn.setOnClickListener { applyScientific("ln") }
        binding.btnSquare.setOnClickListener { applyScientific("x²") }
        binding.btnCube.setOnClickListener { applyScientific("x³") }
        binding.btnSqrt.setOnClickListener { applyScientific("√") }
        binding.btnReciprocal.setOnClickListener { applyScientific("1/x") }
        binding.btnFactorial.setOnClickListener { applyScientific("!") }
        binding.btnPi.setOnClickListener { insertConstantPI() }
        binding.btnAngleMode.setOnClickListener {
            angleModeDegrees = !angleModeDegrees
            updateScientificPanel()
        }
        binding.btnInverse.setOnClickListener {
            inverseMode = !inverseMode
            updateScientificPanel()
        }
    }

    private fun updateScientificPanel() {
        binding.scientificPanel.visibility = if (scientificMode) View.VISIBLE else View.GONE
        binding.btnAngleMode.text = if (angleModeDegrees) "deg" else "rad"
        binding.btnInverse.text = if (inverseMode) "inv*" else "inv"
        updateTopBarDescriptions()
    }

    private fun updateTopBarDescriptions() {
        binding.btnOverflowMenu.contentDescription = getString(R.string.open_menu)
        binding.btnMoreCalculators.contentDescription = getString(R.string.open_calculators)
        binding.btnScientificMode.contentDescription = if (scientificMode) {
            getString(R.string.hide_scientific_mode)
        } else {
            getString(R.string.show_scientific_mode)
        }
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

        lastShareExpression = ""
        lastShareResult = ""
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

        lastShareExpression = ""
        lastShareResult = ""
        justEvaluated = false
        renderDisplay()
    }

    private fun percentPressed() {
        val input = currentInputValue()
        val value = input / 100.0
        currentInput = CalculatorMath.formatNumber(value)
        lastShareExpression = "${CalculatorMath.formatNumber(input)}%"
        lastShareResult = currentInput
        HistoryStore.add(this, lastShareExpression, lastShareResult)
        justEvaluated = false
        renderDisplay()
    }

    private fun backspacePressed() {
        if (currentInput == "Error" || currentInput == "0") {
            if (pendingOperator != null && leftOperand != null) {
                currentInput = CalculatorMath.formatNumber(leftOperand!!)
                leftOperand = null
                pendingOperator = null
                justEvaluated = false
                lastShareExpression = ""
                lastShareResult = ""
                renderDisplay()
                return
            }

            currentInput = "0"
            renderDisplay()
            return
        }

        currentInput = when {
            currentInput.length <= 1 -> "0"
            currentInput.length == 2 && currentInput.startsWith("-") -> "0"
            else -> currentInput.dropLast(1)
        }

        if (currentInput == "0" && pendingOperator != null && leftOperand != null) {
            currentInput = CalculatorMath.formatNumber(leftOperand!!)
            leftOperand = null
            pendingOperator = null
        }

        if (currentInput == "-" || currentInput.isBlank()) {
            currentInput = "0"
        }

        lastShareExpression = ""
        lastShareResult = ""
        renderDisplay()
    }

    private fun onOperatorPressed(operator: String) {
        val current = currentInputValue()

        if (leftOperand == null) {
            leftOperand = current
        } else if (pendingOperator != null && !justEvaluated) {
            leftOperand = CalculatorMath.calculateBinary(leftOperand!!, current, pendingOperator!!)
        }

        pendingOperator = operator
        currentInput = "0"
        lastShareExpression = ""
        lastShareResult = ""
        justEvaluated = false
        renderDisplay()
    }

    private fun onEqualsPressed() {
        val operator = pendingOperator ?: return
        val left = leftOperand ?: currentInputValue()
        val right = currentInputValue()
        val result = CalculatorMath.calculateBinary(left, right, operator)

        currentInput = CalculatorMath.formatNumber(result)
        lastShareExpression = "${CalculatorMath.formatNumber(left)} $operator ${CalculatorMath.formatNumber(right)}"
        lastShareResult = currentInput
        HistoryStore.add(this, lastShareExpression, lastShareResult)

        leftOperand = null
        pendingOperator = null
        justEvaluated = true
        renderDisplay()
    }

    private fun applyScientific(function: String) {
        val value = currentInputValue()
        val result = CalculatorMath.applyUnary(value, function, angleModeDegrees, inverseMode)
        currentInput = CalculatorMath.formatNumber(result)
        lastShareExpression = when (function) {
            "x²" -> "${CalculatorMath.formatNumber(value)}²"
            "x³" -> "${CalculatorMath.formatNumber(value)}³"
            "1/x" -> "1 / ${CalculatorMath.formatNumber(value)}"
            "!" -> "${CalculatorMath.formatNumber(value)}!"
            "√" -> "√${CalculatorMath.formatNumber(value)}"
            else -> "$function(${CalculatorMath.formatNumber(value)})"
        }
        lastShareResult = currentInput
        HistoryStore.add(this, lastShareExpression, lastShareResult)

        leftOperand = null
        pendingOperator = null
        justEvaluated = true
        renderDisplay()
    }

    private fun insertConstantPI() {
        currentInput = CalculatorMath.formatNumber(PI)
        lastShareExpression = ""
        lastShareResult = ""
        justEvaluated = false
        renderDisplay()
    }

    private fun clearAll() {
        currentInput = "0"
        leftOperand = null
        pendingOperator = null
        justEvaluated = false
        lastShareExpression = ""
        lastShareResult = ""
        renderDisplay()
    }

    private fun currentInputValue(): Double {
        return CalculatorMath.parseDouble(normalizeNumericString(currentInput))
    }

    private fun normalizeNumericString(value: String): String {
        return when {
            value.isBlank() -> "0"
            value == "-" -> "0"
            value.endsWith(".") -> value.dropLast(1)
            else -> value
        }
    }

    private fun buildExpressionText(): String {
        return when {
            leftOperand != null && pendingOperator != null -> {
                val left = CalculatorMath.formatNumber(leftOperand!!)
                if (currentInput == "0" && !justEvaluated) {
                    "$left $pendingOperator"
                } else {
                    "$left $pendingOperator ${currentInput}"
                }
            }
            else -> currentInput
        }
    }

    private fun buildPreviewText(): String {
        return when {
            leftOperand != null && pendingOperator != null && currentInput != "0" ->
                CalculatorMath.formatNumber(
                    CalculatorMath.calculateBinary(leftOperand!!, currentInputValue(), pendingOperator!!)
                )
            else -> ""
        }
    }

    private fun currentShareText(): String {
        val expression = if (lastShareExpression.isNotBlank()) lastShareExpression else buildExpressionText()
        val result = if (lastShareResult.isNotBlank()) lastShareResult else buildPreviewText().ifBlank { currentInput }
        return "Calculation:\n$expression = $result\nCalculated using ${getString(R.string.app_name)}"
    }

    private fun currentResultText(): String {
        return if (lastShareResult.isNotBlank()) {
            lastShareResult
        } else {
            buildPreviewText().ifBlank { currentInput }
        }
    }

    private fun copyCurrentResult() {
        copyToClipboard(getString(R.string.app_name), currentResultText())
    }

    private fun shareCurrentResult() {
        sharePlainText(getString(R.string.app_name), currentShareText())
    }

    private fun saveCurrentResult() {
        val expression = if (lastShareExpression.isNotBlank()) lastShareExpression else buildExpressionText()
        val result = if (lastShareResult.isNotBlank()) lastShareResult else buildPreviewText().ifBlank { currentInput }
        HistoryStore.add(this, expression, result)
    }

    private fun renderDisplay() {
        binding.expressionText.text = buildExpressionText()
        binding.previewText.text = buildPreviewText()
        binding.previewText.alpha = if (binding.previewText.text.isNullOrEmpty()) 0f else 1f

        val showCursor = pendingOperator != null || currentInput != "0" || justEvaluated
    }
}
