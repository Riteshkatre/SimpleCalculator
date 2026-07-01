package com.riteshkatre.simplecalculator

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivityCalculatorsBinding

class CalculatorsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalculatorsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityCalculatorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.cardGst.setOnClickListener { open(GstCalculatorActivity::class.java) }
        binding.cardPercentage.setOnClickListener { open(PercentageCalculatorActivity::class.java) }
        binding.cardEmi.setOnClickListener { open(EmiCalculatorActivity::class.java) }
        binding.cardUnit.setOnClickListener { open(UnitConverterActivity::class.java) }
        binding.cardAge.setOnClickListener { open(AgeCalculatorActivity::class.java) }
        binding.cardNumberSystem.setOnClickListener { open(NumberSystemConverterActivity::class.java) }
        binding.cardCurrency.setOnClickListener { open(CurrencyConverterActivity::class.java) }
    }

    private fun open(target: Class<*>) {
        startActivity(Intent(this, target))
    }
}
