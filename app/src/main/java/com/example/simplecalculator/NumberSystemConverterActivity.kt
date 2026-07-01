package com.riteshkatre.simplecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivityNumberSystemConverterBinding
import java.math.BigInteger

class NumberSystemConverterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNumberSystemConverterBinding
    private var currentBinaryInput = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityNumberSystemConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnClear.setOnClickListener { clearAll() }
        binding.btnBackspace.setOnClickListener { backspacePressed() }

        binding.btnOne.setOnClickListener { onDigitPressed("1") }
        binding.btnZero.setOnClickListener { onDigitPressed("0") }
        binding.btnDoubleZero.setOnClickListener { onDigitPressed("00") }

        binding.btnCopy.setOnClickListener { copyToClipboard(getString(R.string.app_name), currentShareText()) }
        binding.btnShare.setOnClickListener { sharePlainText(getString(R.string.app_name), currentShareText()) }
        binding.btnSave.setOnClickListener { saveCurrentResult() }

        renderDisplay()
    }

    private fun onDigitPressed(digit: String) {
        currentBinaryInput = when {
            currentBinaryInput == "0" && digit == "1" -> "1"
            currentBinaryInput == "0" && digit == "0" -> "0"
            currentBinaryInput == "0" && digit == "00" -> "0"
            else -> currentBinaryInput + digit
        }

        renderDisplay()
    }

    private fun backspacePressed() {
        currentBinaryInput = when {
            currentBinaryInput.length <= 1 -> "0"
            else -> currentBinaryInput.dropLast(1).ifBlank { "0" }
        }
        renderDisplay()
    }

    private fun clearAll() {
        currentBinaryInput = "0"
        renderDisplay()
    }

    private fun currentDecimalValue(): String {
        return runCatching {
            BigInteger(currentBinaryInput.ifBlank { "0" }, 2).toString(10)
        }.getOrElse {
            "Error"
        }
    }

    private fun currentShareText(): String {
        val decimalValue = currentDecimalValue()
        return "Number System Conversion:\nBinary: $currentBinaryInput\nDecimal: $decimalValue\nCalculated using ${getString(R.string.app_name)}"
    }

    private fun saveCurrentResult() {
        val decimalValue = currentDecimalValue()
        HistoryStore.add(this, "Binary $currentBinaryInput", decimalValue)
    }

    private fun renderDisplay() {
        binding.binaryValue.text = currentBinaryInput
        binding.decimalValue.text = currentDecimalValue()
    }
}
