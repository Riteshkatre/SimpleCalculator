package com.riteshkatre.simplecalculator

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivityUnitConverterBinding
import kotlin.math.pow

class UnitConverterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUnitConverterBinding
    private val categories = listOf("Length", "Weight", "Temperature", "Area")

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityUnitConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        setupCategorySpinner()
        binding.btnConvert.setOnClickListener { convert() }
        binding.btnCopy.setOnClickListener { copyToClipboard(getString(R.string.app_name), binding.resultSummary.text.toString()) }
        binding.btnShare.setOnClickListener { sharePlainText(getString(R.string.app_name), binding.resultSummary.text.toString()) }
        binding.btnSave.setOnClickListener { saveResult() }
    }

    private fun setupCategorySpinner() {
        binding.categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.categorySpinner.setSelection(0)
        updateUnitSpinners()
        binding.categorySpinner.setOnItemSelectedListener(SimpleItemSelectedListener {
            updateUnitSpinners()
        })
    }

    private fun updateUnitSpinners() {
        val units = when (binding.categorySpinner.selectedItem.toString()) {
            "Length" -> listOf("meter", "km", "feet", "inch")
            "Weight" -> listOf("kg", "gram", "pound")
            "Temperature" -> listOf("Celsius", "Fahrenheit")
            else -> listOf("sq ft", "acre", "hectare")
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, units)
        binding.fromSpinner.adapter = adapter
        binding.toSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, units)
        binding.toSpinner.setSelection((units.size - 1).coerceAtLeast(0))
    }

    private fun convert() {
        val value = CalculatorMath.parseDouble(binding.valueInput.text?.toString().orEmpty())
        val category = binding.categorySpinner.selectedItem.toString()
        val from = binding.fromSpinner.selectedItem.toString()
        val to = binding.toSpinner.selectedItem.toString()
        val result = convertValue(category, from, to, value)
        binding.resultSummary.text = "${CalculatorMath.formatNumber(value)} $from = ${CalculatorMath.formatNumber(result)} $to"
        binding.resultCard.visibility = android.view.View.VISIBLE
        HistoryStore.add(this, binding.resultSummary.text.toString(), CalculatorMath.formatNumber(result))
    }

    private fun convertValue(category: String, from: String, to: String, value: Double): Double {
        if (from == to) return value
        return when (category) {
            "Length" -> {
                val meters = when (from) {
                    "km" -> value * 1000
                    "feet" -> value * 0.3048
                    "inch" -> value * 0.0254
                    else -> value
                }
                when (to) {
                    "km" -> meters / 1000
                    "feet" -> meters / 0.3048
                    "inch" -> meters / 0.0254
                    else -> meters
                }
            }
            "Weight" -> {
                val kg = when (from) {
                    "gram" -> value / 1000
                    "pound" -> value * 0.45359237
                    else -> value
                }
                when (to) {
                    "gram" -> kg * 1000
                    "pound" -> kg / 0.45359237
                    else -> kg
                }
            }
            "Temperature" -> {
                val celsius = when (from) {
                    "Fahrenheit" -> (value - 32) * 5 / 9
                    else -> value
                }
                when (to) {
                    "Fahrenheit" -> celsius * 9 / 5 + 32
                    else -> celsius
                }
            }
            else -> {
                val squareFeet = when (from) {
                    "acre" -> value * 43560
                    "hectare" -> value * 107639.104167
                    else -> value
                }
                when (to) {
                    "acre" -> squareFeet / 43560
                    "hectare" -> squareFeet / 107639.104167
                    else -> squareFeet
                }
            }
        }
    }

    private fun saveResult() {
        val summary = binding.resultSummary.text?.toString().orEmpty()
        if (summary.isNotBlank()) {
            HistoryStore.add(this, summary, summary.substringAfter("=").trim())
        }
    }
}
