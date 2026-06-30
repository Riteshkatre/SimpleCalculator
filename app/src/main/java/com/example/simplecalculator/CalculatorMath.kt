package com.riteshkatre.simplecalculator

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object CalculatorMath {

    fun parseDouble(text: String): Double {
        val normalized = text.replace(",", "").trim()
        return normalized.toDoubleOrNull() ?: 0.0
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) {
            return "Error"
        }

        val normalized = if (abs(value) < 0.0000001) 0.0 else value
        val rounded = BigDecimal.valueOf(normalized)
            .setScale(10, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

        val parts = rounded.split(".")
        val integerPart = parts[0]
        val formattedInteger = groupIntegerPart(integerPart)
        return formattedInteger + parts.getOrNull(1)?.let { ".$it" }.orEmpty()
    }

    fun calculateBinary(left: Double, right: Double, operator: String): Double {
        return when (operator) {
            "+" -> left + right
            "−", "-" -> left - right
            "×", "*" -> left * right
            "÷", "/" -> if (right == 0.0) Double.NaN else left / right
            else -> right
        }
    }

    fun applyUnary(value: Double, function: String, isDegrees: Boolean, inverse: Boolean): Double {
        return when (function) {
            "sin" -> if (inverse) {
                fromAngle(asin(value), isDegrees)
            } else {
                sin(toAngle(value, isDegrees))
            }
            "cos" -> if (inverse) {
                fromAngle(acos(value), isDegrees)
            } else {
                cos(toAngle(value, isDegrees))
            }
            "tan" -> if (inverse) {
                fromAngle(atan(value), isDegrees)
            } else {
                tan(toAngle(value, isDegrees))
            }
            "log" -> if (value <= 0.0) Double.NaN else log10(value)
            "ln" -> if (value <= 0.0) Double.NaN else ln(value)
            "√" -> if (value < 0.0) Double.NaN else sqrt(value)
            "x²" -> value.pow(2.0)
            "x³" -> value.pow(3.0)
            "1/x" -> if (value == 0.0) Double.NaN else 1 / value
            "!" -> factorial(value)
            else -> value
        }
    }

    fun factorial(value: Double): Double {
        if (value < 0 || value != value.roundToLong().toDouble()) {
            return Double.NaN
        }

        var result = 1.0
        val target = value.roundToLong()
        for (index in 2..target) {
            result *= index
            if (result.isInfinite()) return Double.NaN
        }
        return result
    }

    fun percentOf(base: Double, percent: Double): Double = base * percent / 100.0

    fun percentageAdjustment(base: Double, percent: Double, add: Boolean): Double {
        val delta = percentOf(base, percent)
        return if (add) base + delta else base - delta
    }

    private fun toAngle(value: Double, isDegrees: Boolean): Double {
        return if (isDegrees) Math.toRadians(value) else value
    }

    private fun fromAngle(value: Double, isDegrees: Boolean): Double {
        return if (isDegrees) Math.toDegrees(value) else value
    }

    private fun groupIntegerPart(value: String): String {
        val negative = value.startsWith("-")
        val digits = value.removePrefix("-")
        if (digits.length <= 3) {
            return value
        }

        val builder = StringBuilder()
        var index = digits.length
        while (index > 3) {
            val start = index - 3
            builder.insert(0, ",${digits.substring(start, index)}")
            index = start
        }
        builder.insert(0, digits.substring(0, index))
        return if (negative) "-$builder" else builder.toString()
    }
}
