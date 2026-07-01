package com.riteshkatre.simplecalculator

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivityAgeCalculatorBinding
import java.util.Calendar

class AgeCalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgeCalculatorBinding
    private var selectedDob: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityAgeCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPickDate.setOnClickListener { pickDate() }
        binding.btnCalculate.setOnClickListener { calculateAge() }
        binding.btnCopy.setOnClickListener { copyToClipboard(getString(R.string.app_name), binding.resultSummary.text.toString()) }
        binding.btnShare.setOnClickListener { sharePlainText(getString(R.string.app_name), binding.resultSummary.text.toString()) }
        binding.btnSave.setOnClickListener { saveResult() }
    }

    private fun pickDate() {
        val now = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDob = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                binding.dateText.text = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
            },
            now.get(Calendar.YEAR) - 25,
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun calculateAge() {
        val dob = selectedDob ?: run {
            binding.resultSummary.text = "Please choose a birth date."
            binding.resultCard.visibility = android.view.View.VISIBLE
            return
        }

        val today = Calendar.getInstance()
        val birth = dob.clone() as Calendar
        val nextBirthdayDate = dob.clone() as Calendar
        var years = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        var months = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
        var days = today.get(Calendar.DAY_OF_MONTH) - birth.get(Calendar.DAY_OF_MONTH)

        if (days < 0) {
            birth.add(Calendar.MONTH, -1)
            days += birth.getActualMaximum(Calendar.DAY_OF_MONTH)
            months -= 1
        }
        if (months < 0) {
            months += 12
            years -= 1
        }

        nextBirthdayDate.set(Calendar.YEAR, today.get(Calendar.YEAR))
        if (isBirthdayPassedThisYear(today, nextBirthdayDate)) {
            nextBirthdayDate.add(Calendar.YEAR, 1)
        }
        val daysUntilBirthday = daysBetween(today, nextBirthdayDate)

        binding.resultYears.text = years.toString()
        binding.resultMonths.text = months.toString()
        binding.resultDays.text = days.toString()
        binding.resultNextBirthday.text = if (daysUntilBirthday == 1L) "1 day" else "$daysUntilBirthday days"
        binding.resultSummary.text = "Age: $years years, $months months, $days days"
        binding.resultCard.visibility = android.view.View.VISIBLE
        HistoryStore.add(this, "DOB ${binding.dateText.text}", binding.resultSummary.text.toString())
    }

    private fun isBirthdayPassedThisYear(today: Calendar, birthdayThisYear: Calendar): Boolean {
        val todayNoTime = today.clone() as Calendar
        todayNoTime.set(Calendar.HOUR_OF_DAY, 0)
        todayNoTime.set(Calendar.MINUTE, 0)
        todayNoTime.set(Calendar.SECOND, 0)
        todayNoTime.set(Calendar.MILLISECOND, 0)

        val birthdayNoTime = birthdayThisYear.clone() as Calendar
        birthdayNoTime.set(Calendar.HOUR_OF_DAY, 0)
        birthdayNoTime.set(Calendar.MINUTE, 0)
        birthdayNoTime.set(Calendar.SECOND, 0)
        birthdayNoTime.set(Calendar.MILLISECOND, 0)

        return birthdayNoTime.before(todayNoTime)
    }

    private fun daysBetween(start: Calendar, end: Calendar): Long {
        val startNoTime = start.clone() as Calendar
        startNoTime.set(Calendar.HOUR_OF_DAY, 0)
        startNoTime.set(Calendar.MINUTE, 0)
        startNoTime.set(Calendar.SECOND, 0)
        startNoTime.set(Calendar.MILLISECOND, 0)

        val endNoTime = end.clone() as Calendar
        endNoTime.set(Calendar.HOUR_OF_DAY, 0)
        endNoTime.set(Calendar.MINUTE, 0)
        endNoTime.set(Calendar.SECOND, 0)
        endNoTime.set(Calendar.MILLISECOND, 0)

        return ((endNoTime.timeInMillis - startNoTime.timeInMillis) / (24L * 60L * 60L * 1000L)).coerceAtLeast(0L)
    }

    private fun saveResult() {
        val summary = binding.resultSummary.text?.toString().orEmpty()
        if (summary.isNotBlank()) {
            HistoryStore.add(this, "DOB ${binding.dateText.text}", summary)
        }
    }
}
