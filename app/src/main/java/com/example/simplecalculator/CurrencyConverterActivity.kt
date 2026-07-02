package com.riteshkatre.simplecalculator

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivityCurrencyConverterBinding
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import org.json.JSONObject

class CurrencyConverterActivity : AppCompatActivity() {

    private data class CurrencyOption(
        val label: String,
        val code: String,
    ) {
        override fun toString(): String = "$label $code"
    }

    private data class FxRate(
        val fromCode: String,
        val toCode: String,
        val rate: BigDecimal,
        val fetchedAtLabel: String,
    )

    private lateinit var binding: ActivityCurrencyConverterBinding
    private val mainHandler = Handler(Looper.getMainLooper())
    private val networkExecutor = Executors.newSingleThreadExecutor()
    private val currencies = listOf(
        CurrencyOption("US Dollar", "USD"),
        CurrencyOption("Indian Rupee", "INR"),
        CurrencyOption("Euro", "EUR"),
        CurrencyOption("British Pound", "GBP"),
        CurrencyOption("Japanese Yen", "JPY"),
        CurrencyOption("UAE Dirham", "AED"),
        CurrencyOption("Canadian Dollar", "CAD"),
        CurrencyOption("Australian Dollar", "AUD"),
    )

    private var currentInput = "100"
    private var lastUpdatedText = ""
    private var liveRate: FxRate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityCurrencyConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnClear.setOnClickListener { clearAll() }
        binding.btnBackspace.setOnClickListener { backspacePressed() }
        binding.btnSwap.setOnClickListener { swapCurrencies() }
        binding.btnRefresh.setOnClickListener { fetchLiveRates() }

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
        binding.btnDecimal.setOnClickListener { appendDecimal() }

        setupCurrencySpinners()
        AdManager.loadBanner(this, binding.bannerAdContainer)
        fetchLiveRates()
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
            fetchLiveRates()
        }
        binding.toCurrencySpinner.onItemSelectedListener = SimpleItemSelectedListener {
            fetchLiveRates()
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
        fetchLiveRates()
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

    private fun convert(amount: BigDecimal): BigDecimal {
        val rate = liveRate?.rate ?: return BigDecimal.ZERO
        return amount.multiply(rate).setScale(4, RoundingMode.HALF_UP)
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
        val converted = convert(amount)

        binding.fromAmount.text = formatEditableAmount(currentInput)
        binding.fromCurrencySpinner.prompt = from.toString()
        binding.toAmount.text = if (liveRate == null) "--" else formatAmount(converted)
        binding.toCurrencySpinner.prompt = to.toString()
    }

    private fun fetchLiveRates() {
        binding.lastUpdated.text = "Loading live rates..."
        val fromCode = selectedFrom().code
        val toCode = selectedTo().code
        if (fromCode == toCode) {
            liveRate = FxRate(
                fromCode = fromCode,
                toCode = toCode,
                rate = BigDecimal.ONE,
                fetchedAtLabel = SimpleDateFormat("M/d/yyyy, h:mm a", Locale.getDefault()).format(Date()),
            )
            lastUpdatedText = liveRate!!.fetchedAtLabel
            binding.lastUpdated.text = "Last updated: ${liveRate!!.fetchedAtLabel}"
            renderDisplay()
            return
        }

        networkExecutor.execute {
            val result = runCatching {
                val apiUrl = "https://api.frankfurter.dev/v2/rate/$fromCode/$toCode"
                val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                parseRate(body, fromCode, toCode)
            }.getOrElse {
                mainHandler.post {
                    Toast.makeText(this, "Unable to load live rate", Toast.LENGTH_SHORT).show()
                    binding.lastUpdated.text = "Last updated: unavailable"
                    binding.toAmount.text = "--"
                }
                null
            }

            mainHandler.post {
                if (result != null) {
                    liveRate = result
                    lastUpdatedText = result.fetchedAtLabel
                    binding.lastUpdated.text = "Last updated: ${result.fetchedAtLabel}"
                    renderDisplay()
                }
            }
        }
    }

    private fun parseRate(json: String, fromCode: String, toCode: String): FxRate {
        val root = JSONObject(json)
        val fetchedAtLabel = root.optString("date").takeIf { it.isNotBlank() }
            ?: SimpleDateFormat("M/d/yyyy, h:mm a", Locale.getDefault()).format(Date())
        return FxRate(
            fromCode = fromCode,
            toCode = toCode,
            rate = BigDecimal.valueOf(root.getDouble("rate")),
            fetchedAtLabel = fetchedAtLabel,
        )
    }
}
