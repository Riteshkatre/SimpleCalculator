package com.riteshkatre.simplecalculator

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    fun shareText(appName: String): String = "Calculation:\n$expression = $result\nCalculated using $appName"
}

object HistoryStore {
    private const val PREFS_NAME = "calculator_prefs"
    private const val KEY_HISTORY = "history_entries"
    private const val MAX_ITEMS = 100

    fun load(context: Context): MutableList<HistoryEntry> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "[]")
            .orEmpty()
        val items = mutableListOf<HistoryEntry>()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: JSONArray()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            items += HistoryEntry(
                expression = item.optString("expression"),
                result = item.optString("result"),
                timestamp = item.optLong("timestamp", System.currentTimeMillis())
            )
        }
        return items
    }

    fun add(context: Context, expression: String, result: String) {
        val items = load(context)
        items.add(0, HistoryEntry(expression = expression, result = result))
        save(context, items.take(MAX_ITEMS))
    }

    fun deleteAt(context: Context, index: Int) {
        val items = load(context)
        if (index !in items.indices) return
        items.removeAt(index)
        save(context, items)
    }

    fun clear(context: Context) {
        save(context, emptyList())
    }

    private fun save(context: Context, items: List<HistoryEntry>) {
        val array = JSONArray()
        items.forEach { entry ->
            array.put(JSONObject().apply {
                put("expression", entry.expression)
                put("result", entry.result)
                put("timestamp", entry.timestamp)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY, array.toString())
            .apply()
    }
}

