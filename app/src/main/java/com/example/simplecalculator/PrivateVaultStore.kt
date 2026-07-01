package com.riteshkatre.simplecalculator

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class VaultFileEntry(
    val id: String,
    val originalName: String,
    val folder: String,
    val encryptedName: String,
    val isTrashed: Boolean,
    val createdAt: Long,
)

object PrivateVaultStore {
    private const val PREFS_NAME = "private_vault_prefs"
    private const val KEY_PASSWORD_SALT = "vault_password_salt"
    private const val KEY_PASSWORD_HASH = "vault_password_hash"
    private const val KEY_ENTRIES = "vault_entries"
    private const val KEY_FOLDERS = "vault_folders"
    private const val DEFAULT_FOLDER = "Vault"

    fun hasPassword(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_PASSWORD_HASH) && prefs.contains(KEY_PASSWORD_SALT)
    }

    fun savePassword(context: Context, password: String) {
        val salt = CryptoUtils.randomBytes(16)
        val hash = CryptoUtils.sha256(salt + password.toByteArray(Charsets.UTF_8))
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PASSWORD_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PASSWORD_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_FOLDERS, JSONArray().apply { put(DEFAULT_FOLDER) }.toString())
            .putString(KEY_ENTRIES, "[]")
            .apply()
    }

    fun verifyPassword(context: Context, password: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val salt = prefs.getString(KEY_PASSWORD_SALT, null)?.let {
            runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull()
        } ?: return false
        val expected = prefs.getString(KEY_PASSWORD_HASH, null)?.let {
            runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull()
        } ?: return false
        val actual = CryptoUtils.sha256(salt + password.toByteArray(Charsets.UTF_8))
        return expected.contentEquals(actual)
    }

    fun loadEntries(context: Context): MutableList<VaultFileEntry> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ENTRIES, "[]")
            .orEmpty()
        val items = mutableListOf<VaultFileEntry>()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: JSONArray()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            items += VaultFileEntry(
                id = item.optString("id"),
                originalName = item.optString("originalName"),
                folder = item.optString("folder", DEFAULT_FOLDER),
                encryptedName = item.optString("encryptedName"),
                isTrashed = item.optBoolean("isTrashed", false),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
            )
        }
        return items
    }

    fun saveEntries(context: Context, entries: List<VaultFileEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().apply {
                put("id", entry.id)
                put("originalName", entry.originalName)
                put("folder", entry.folder)
                put("encryptedName", entry.encryptedName)
                put("isTrashed", entry.isTrashed)
                put("createdAt", entry.createdAt)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENTRIES, array.toString())
            .apply()
    }

    fun loadFolders(context: Context): MutableList<String> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FOLDERS, "[]")
            .orEmpty()
        val items = mutableListOf<String>()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: JSONArray()
        for (index in 0 until array.length()) {
            val folder = array.optString(index)
            if (folder.isNotBlank()) items += folder
        }
        if (items.isEmpty()) items += DEFAULT_FOLDER
        return items.distinct().toMutableList()
    }

    fun saveFolders(context: Context, folders: List<String>) {
        val array = JSONArray()
        folders.distinct().forEach { array.put(it) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FOLDERS, array.toString())
            .apply()
    }

    fun defaultFolder(): String = DEFAULT_FOLDER
}
