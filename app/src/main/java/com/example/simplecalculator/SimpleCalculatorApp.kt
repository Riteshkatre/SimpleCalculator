package com.riteshkatre.simplecalculator

import android.app.Application

class SimpleCalculatorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AdManager.initialize(this)
    }
}
