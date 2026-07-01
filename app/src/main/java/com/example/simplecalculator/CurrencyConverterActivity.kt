package com.riteshkatre.simplecalculator

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivityCurrencyConverterBinding
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CurrencyConverterActivity : AppCompatActivity() {

    private data class CurrencyOption(
        val label: String,
        val code: String,
        val rateToUsd: BigDecimal,
    ) {
        override fun toString(): String = "$label $code"
    }

    private lateinit var binding: ActivityCurrencyConverterBinding
    private val currencies = listOf(
        CurrencyOption("US Dollar", "USD", BigDecimal("1.0")),
        CurrencyOption("Indian Rupee", "INR", BigDecimal("94.717016")),
        CurrencyOption("Euro", "EUR", BigDecimal("0.92")),
        CurrencyOption("British Pound", "GBP", BigDecimal("0.79")),
        CurrencyOption("Japanese Yen", "JPY", BigDecimal("144.85")),
        CurrencyOption("UAE Dirham", "AED", BigDecimal("3.67")),
        CurrencyOption("Canadian Dollar", "CAD", BigDecimal("1.36")),
        CurrencyOption("Australian Dollar", "AUD", BigDecimal("1.51")),
    )

    private var currentInput = "100"
    private var lastUpdatedText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityCurrencyConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnClear.setOnClickListener { clearAll() }
        binding.btnBackspace.setOnClickListener { backspacePressed() }
        binding.btnSwap.setOnClickListener { swapCurrencies() }
        binding.btnRefresh.setOnClickListener { updateLastUpdated() }

        binding.btn0.setOnClickListener { appendDigit("0") }
        binding.btn1.setOnClickListener { appendDigit("1") }
        binding.btn2.setOnClickListener { appendDigit("2") }
        binding.btn3.setOnClickListener { appendDigit("3") }
        binding.btn4.setOnClickListener { appendDigit("4") }
        binding.btn5.setOnClickListener { appendDigit("5") }
        binding.btn6.setOnClickListener { appendDigit("6") }
        binding.btn7.setOnClickListener { appendDigit("7") }
        binding.btn8.setOnClickListener { appendDigit("8") }
        binding.btn9.setOnClickListener { appendDigit("9") }
        binding.btnDoubleZero.setOnClickListener { appendDigit("00") }
        binding.btnDecimal.setOnClickListener { appendDecimal() }

        setupCurrencySpinners()
        updateLastUpdated()
        renderDisplay()
    }

    private fun setupCurrencySpinners() {
        val fromAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val toAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.fromCurrencySpinner.adapter = fromAdapter
        binding.toCurrencySpinner.adapter = toAdapter
        binding.fromCurrencySpinner.setSelection(0)
        binding.toCurrencySpinner.setSelection(1)

        binding.fromCurrencySpinner.onItemSelectedListener = SimpleItemSelectedListener {
            renderDisplay()
        }
        binding.toCurrencySpinner.onItemSelectedListener = SimpleItemSelectedListener {
            renderDisplay()
        }
    }

    private fun appendDigit(digit: String) {
        currentInput = when {
            currentInput == "0" && digit == "00" -> "0"
            currentInput == "0" && digit == "0" -> "0"
            currentInput == "0" -> digit
            else -> currentInput + digit
        }
        renderDisplay()
    }

    private fun appendDecimal() {
        currentInput = when {
            currentInput.contains(".") -> currentInput
            currentInput == "0" -> "0."
            else -> "$currentInput."
        }
        renderDisplay()
    }

    private fun backspacePressed() {
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

    private fun clearAll() {
        currentInput = "0"
        renderDisplay()
    }

    private fun swapCurrencies() {
        val from = binding.fromCurrencySpinner.selectedItemPosition
        val to = binding.toCurrencySpinner.selectedItemPosition
        binding.fromCurrencySpinner.setSelection(to)
        binding.toCurrencySpinner.setSelection(from)
        renderDisplay()
    }

    private fun updateLastUpdated() {
        lastUpdatedText = SimpleDateFormat("M/d/yyyy, h:mm a", Locale.getDefault()).format(Date())
        binding.lastUpdated.text = "Last updated: $lastUpdatedText"
    }

    private fun selectedFrom() = binding.fromCurrencySpinner.selectedItem as CurrencyOption

    private fun selectedTo() = binding.toCurrencySpinner.selectedItem as CurrencyOption

    private fun parsedAmount(): BigDecimal {
        return runCatching {
            BigDecimal(normalizeAmount(currentInput))
        }.getOrElse {
            BigDecimal.ZERO
        }
    }

    private fun normalizeAmount(value: String): String {
        return when {
            value.isBlank() -> "0"
            value == "-" -> "0"
            value.endsWith(".") -> value.dropLast(1)
            else -> value
        }
    }

    private fun convert(amount: BigDecimal, from: CurrencyOption, to: CurrencyOption): BigDecimal {
        if (from.code == to.code) return amount
        val usdValue = amount.divide(from.rateToUsd, 10, RoundingMode.HALF_UP)
        return usdValue.multiply(to.rateToUsd).setScale(4, RoundingMode.HALF_UP)
    }

    private fun formatAmount(value: BigDecimal): String {
        val normalized = value.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        val parts = normalized.split(".")
        val integerPart = parts[0]
        val grouped = integerPart.reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
        return if (parts.size > 1) "$grouped.${parts[1]}" else grouped
    }

    private fun formatEditableAmount(value: String): String {
        return when {
            value.endsWith(".") -> formatAmount(BigDecimal(normalizeAmount(value))) + "."
            else -> formatAmount(BigDecimal(normalizeAmount(value)))
        }
    }

    private fun renderDisplay() {
        val from = selectedFrom()
        val to = selectedTo()
        val amount = parsedAmount()
        val converted = convert(amount, from, to)

        binding.fromAmount.text = formatEditableAmount(currentInput)
        binding.fromCurrencySpinner.prompt = from.toString()
        binding.toAmount.text = formatAmount(converted)
        binding.toCurrencySpinner.prompt = to.toString()
    }
}
