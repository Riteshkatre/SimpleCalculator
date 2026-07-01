package com.riteshkatre.simplecalculator

import android.app.AlertDialog
import android.content.ClipData
import android.content.ContentValues
import android.database.Cursor
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.media.MediaScannerConnection
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.riteshkatre.simplecalculator.databinding.ActivityPrivateVaultBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.Locale
import android.webkit.MimeTypeMap
import javax.crypto.CipherOutputStream
import javax.crypto.CipherInputStream

class PrivateVaultActivity : AppCompatActivity() {
    private companion object {
        const val IMPORT_BATCH_LIMIT = 3
    }

    private enum class EntryAction { MOVE, COPY }
    private data class SharedImportResult(
        val importedCount: Int,
        val removedCount: Int,
        val failedCount: Int,
    )

    private lateinit var binding: ActivityPrivateVaultBinding
    private var isUnlocked = false
    private var currentFolder = PrivateVaultStore.defaultFolder()
    private var folders = mutableListOf<String>()
    private var entries = mutableListOf<VaultFileEntry>()
    private var pendingSharedUris = mutableListOf<Uri>()
    private var pendingImportUris = mutableListOf<Uri>()
    private var pendingImportRemovesFromSource = true
    private var importFlowActive = false
    private var pendingDeleteToast: String? = null

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            startImportFlow(uris, removeFromSource = true)
        }
    }

    private val deleteSharedLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val message = if (result.resultCode == RESULT_OK) {
            pendingDeleteToast ?: "Original shared files removed from system storage"
        } else {
            "Imported to vault, but system deletion was cancelled"
        }
        pendingDeleteToast = null
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityPrivateVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPasswordAction.setOnClickListener { handlePasswordAction() }
        binding.btnImportFile.setOnClickListener { openDocumentLauncher.launch(arrayOf("image/*")) }
        binding.btnNewFolder.setOnClickListener { promptNewFolder() }
        binding.btnLockVault.setOnClickListener { lockVault() }
        AdManager.loadBanner(this, binding.bannerAdContainer)

        setupInitialState()
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (isUnlocked) {
            loadVault()
        } else {
            updateLockedUi()
        }
    }

    private fun setupInitialState() {
        if (PrivateVaultStore.hasPassword(this)) {
            showUnlockUi()
        } else {
            showSetupUi()
        }
    }

    private fun handlePasswordAction() {
        val password = binding.passcodeInput.text?.toString().orEmpty()
        if (password.length < 4) {
            binding.statusText.text = "Use at least 4 characters."
            return
        }

        if (!PrivateVaultStore.hasPassword(this)) {
            PrivateVaultStore.savePassword(this, password)
            Toast.makeText(this, "Vault passcode created", Toast.LENGTH_SHORT).show()
            isUnlocked = true
            processPendingSharesIfNeeded()
            return
        }

        if (PrivateVaultStore.verifyPassword(this, password)) {
            isUnlocked = true
            processPendingSharesIfNeeded()
        } else {
            binding.statusText.text = "Wrong passcode. Try again."
        }
    }

    private fun lockVault() {
        isUnlocked = false
        binding.passcodeInput.setText("")
        setupInitialState()
    }

    private fun showSetupUi() {
        binding.statusText.text = "Create a passcode to protect your files."
        binding.passcodeLabel.text = "Set Passcode"
        binding.btnPasswordAction.text = "Create Vault"
        binding.vaultSection.visibility = View.GONE
        binding.passwordCard.visibility = View.VISIBLE
        applyPendingShareHint()
    }

    private fun showUnlockUi() {
        binding.statusText.text = "Enter your passcode to unlock the vault."
        binding.passcodeLabel.text = "Passcode"
        binding.btnPasswordAction.text = "Unlock Vault"
        binding.vaultSection.visibility = View.GONE
        binding.passwordCard.visibility = View.VISIBLE
        applyPendingShareHint()
    }

    private fun updateLockedUi() {
        if (PrivateVaultStore.hasPassword(this)) {
            showUnlockUi()
        } else {
            showSetupUi()
        }
    }

    private fun applyPendingShareHint() {
        if (pendingSharedUris.isEmpty()) return
        binding.statusText.text = if (PrivateVaultStore.hasPassword(this)) {
            "Shared files are ready. Unlock the vault to import them."
        } else {
            "Shared files detected. Create or enter your passcode to import them."
        }
    }

    private fun loadVault() {
        folders = PrivateVaultStore.loadFolders(this)
        entries = PrivateVaultStore.loadEntries(this)
        binding.passwordCard.visibility = View.GONE
        binding.vaultSection.visibility = View.VISIBLE
        binding.vaultWelcomeText.text = "Files are stored privately inside the app."
        updateFolderSpinner()
        renderEntries()
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val incomingUris = extractSharedUris(intent)
        if (incomingUris.isEmpty()) return
        pendingSharedUris.clear()
        pendingSharedUris.addAll(incomingUris)
        if (isUnlocked) {
            processPendingSharesIfNeeded()
        } else {
            applyPendingShareHint()
        }
    }

    private fun extractSharedUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val uris = mutableListOf<Uri>()

        when (intent.action) {
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris += it }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris += it }
            }
        }

        intent.clipData?.let { clipData: ClipData ->
            for (index in 0 until clipData.itemCount) {
                clipData.getItemAt(index)?.uri?.let { uris += it }
            }
        }

        return uris.distinct()
    }

    private fun processPendingSharesIfNeeded() {
        if (pendingSharedUris.isEmpty()) {
            loadVault()
            return
        }

        val sharedUris = pendingSharedUris.toList()
        pendingSharedUris.clear()
        startImportFlow(sharedUris, removeFromSource = true)
    }

    private fun startImportFlow(uris: List<Uri>, removeFromSource: Boolean) {
        if (uris.isEmpty()) {
            loadVault()
            return
        }

        pendingImportUris.clear()
        pendingImportUris.addAll(uris.distinct())
        pendingImportRemovesFromSource = removeFromSource

        if (!importFlowActive) {
            importFlowActive = true
            consumeImportQueue()
        }
    }

    private fun consumeImportQueue() {
        if (!isUnlocked) {
            importFlowActive = false
            return
        }

        if (pendingImportUris.isEmpty()) {
            importFlowActive = false
            loadVault()
            return
        }

        val batchSize = minOf(IMPORT_BATCH_LIMIT, pendingImportUris.size)
        val batch = pendingImportUris.take(batchSize)
        pendingImportUris = pendingImportUris.drop(batchSize).toMutableList()

        val result = importUrisBatch(batch, pendingImportRemovesFromSource)
        loadVault()

        val message = buildString {
            if (result.importedCount > 0) {
                append("Imported ${result.importedCount} file")
                if (result.importedCount > 1) append("s")
                append(" to vault")
            } else {
                append("No files were imported")
            }
            if (pendingImportUris.isNotEmpty()) {
                append(". Watch a rewarded ad to import ${pendingImportUris.size} more")
                if (pendingImportUris.size > 1) append(" files")
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        if (pendingImportUris.isNotEmpty()) {
            importFlowActive = false
            promptRewardToContinue()
        } else {
            importFlowActive = false
        }
    }

    private fun promptRewardToContinue() {
        var rewardEarned = false
        val shown = AdManager.showRewarded(
            this,
            onRewardEarned = {
                rewardEarned = true
            },
            onFinished = {
                if (rewardEarned) {
                    importFlowActive = true
                    consumeImportQueue()
                } else if (pendingImportUris.isNotEmpty()) {
                    pendingImportUris.clear()
                    Toast.makeText(
                        this,
                        "Watch a rewarded ad next time to import more than 3 files.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )

        if (!shown) {
            importFlowActive = false
        }
    }

    private fun updateFolderSpinner() {
        val displayFolders = folders.toMutableList().apply { add("Trash") }
        if (!displayFolders.contains(currentFolder) && currentFolder != "Trash") {
            currentFolder = PrivateVaultStore.defaultFolder()
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayFolders).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.folderSpinner.adapter = adapter
        val selectionIndex = displayFolders.indexOf(currentFolder).takeIf { it >= 0 } ?: 0
        binding.folderSpinner.setSelection(selectionIndex)
        binding.folderSpinner.setOnItemSelectedListener(SimpleItemSelectedListener {
            currentFolder = binding.folderSpinner.selectedItem.toString()
            renderEntries()
        })
    }

    private fun renderEntries() {
        binding.vaultListContainer.removeAllViews()
        val visibleEntries = entries.filter {
            if (currentFolder == "Trash") it.isTrashed else !it.isTrashed && it.folder == currentFolder
        }.sortedByDescending { it.createdAt }

        binding.emptyStateText.visibility = if (visibleEntries.isEmpty()) View.VISIBLE else View.GONE
        if (visibleEntries.isEmpty()) {
            binding.emptyStateText.text = if (currentFolder == "Trash") {
                "Trash is empty."
            } else {
                "No files in this folder."
            }
        }

        visibleEntries.forEach { entry ->
            binding.vaultListContainer.addView(createEntryCard(entry))
        }
    }

    private fun createEntryCard(entry: VaultFileEntry): View {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            radius = dp(24).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(getColor(R.color.card_background))
            isClickable = true
            isFocusable = true
            setOnClickListener { openEntry(entry) }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val title = MaterialTextView(this).apply {
            text = entry.originalName
            setTextColor(getColor(R.color.display_text))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val subtitle = MaterialTextView(this).apply {
            text = if (entry.isTrashed) "Trash" else "Folder: ${entry.folder}"
            setTextColor(getColor(R.color.secondary_text))
            textSize = 13f
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(14), 0, 0)
        }

        val moveButton = MaterialButton(this).apply {
            text = if (entry.isTrashed) "Restore" else "Move"
            isAllCaps = false
            setBackgroundColor(getColor(R.color.screen_background))
            setTextColor(getColor(R.color.display_text))
            setOnClickListener {
                if (entry.isTrashed) {
                    restoreEntry(entry)
                } else {
                    showFolderActionDialog(entry, EntryAction.MOVE)
                }
            }
        }

        val copyButton = MaterialButton(this).apply {
            text = "Copy"
            isAllCaps = false
            setBackgroundColor(getColor(R.color.screen_background))
            setTextColor(getColor(R.color.display_text))
            setOnClickListener {
                if (!entry.isTrashed) {
                    showFolderActionDialog(entry, EntryAction.COPY)
                }
            }
        }

        val deleteButton = MaterialButton(this).apply {
            text = if (entry.isTrashed) "Delete" else "Trash"
            isAllCaps = false
            setBackgroundColor(getColor(R.color.screen_background))
            setTextColor(getColor(R.color.display_text))
            setOnClickListener {
                if (entry.isTrashed) {
                    deleteEntryPermanently(entry)
                } else {
                    moveToTrash(entry)
                }
            }
        }

        listOf(moveButton, copyButton, deleteButton).forEachIndexed { index, button ->
            (button.layoutParams as? LinearLayout.LayoutParams)?.let { }
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (index > 0) marginStart = dp(8)
            }
            actions.addView(button, params)
        }

        container.addView(title)
        container.addView(subtitle)
        container.addView(actions)
        card.addView(container)
        return card
    }

    private fun moveToTrash(entry: VaultFileEntry) {
        val exported = exportToSystem(entry)
        if (exported) {
            File(vaultDir(), entry.encryptedName).delete()
            entries = entries.filterNot { it.id == entry.id }.toMutableList()
            persistEntries()
            renderEntries()
            Toast.makeText(this, "File returned to system storage", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Unable to export file to system storage", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreEntry(entry: VaultFileEntry) {
        entries = entries.map {
            if (it.id == entry.id) it.copy(isTrashed = false) else it
        }.toMutableList()
        persistEntries()
        renderEntries()
    }

    private fun deleteEntryPermanently(entry: VaultFileEntry) {
        val vaultDir = vaultDir()
        File(vaultDir, entry.encryptedName).delete()
        entries = entries.filterNot { it.id == entry.id }.toMutableList()
        persistEntries()
        renderEntries()
    }

    private fun showFolderActionDialog(entry: VaultFileEntry, action: EntryAction) {
        val targetFolders = folders.filterNot { it == entry.folder }.toMutableList()
        if (targetFolders.isEmpty()) {
            Toast.makeText(this, "Create another folder first.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(if (action == EntryAction.MOVE) "Move file to" else "Copy file to")
            .setItems(targetFolders.toTypedArray()) { _, which ->
                val targetFolder = targetFolders[which]
                if (action == EntryAction.MOVE) {
                    entries = entries.map {
                        if (it.id == entry.id) it.copy(folder = targetFolder, isTrashed = false) else it
                    }.toMutableList()
                } else {
                    copyEntry(entry, targetFolder)
                    return@setItems
                }
                persistEntries()
                renderEntries()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun copyEntry(entry: VaultFileEntry, targetFolder: String) {
        val source = File(vaultDir(), entry.encryptedName)
        if (!source.exists()) return
        val newId = UUID.randomUUID().toString()
        val newName = "$newId.bin"
        source.copyTo(File(vaultDir(), newName), overwrite = true)
        entries.add(
            entry.copy(
                id = newId,
                folder = targetFolder,
                encryptedName = newName,
                isTrashed = false,
            )
        )
        persistEntries()
        renderEntries()
    }

    private fun promptNewFolder() {
        val input = EditText(this).apply {
            hint = "Folder name"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(this)
            .setTitle("Create Hidden Folder")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val folder = input.text?.toString()?.trim().orEmpty()
                if (folder.isNotBlank() && !folders.contains(folder)) {
                    folders.add(folder)
                    PrivateVaultStore.saveFolders(this, folders)
                    updateFolderSpinner()
                    Toast.makeText(this, "Folder created", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importUrisBatch(uris: List<Uri>, removeFromSource: Boolean): SharedImportResult {
        var importedCount = 0
        var removedCount = 0
        var failedCount = 0

        uris.forEach { uri ->
            val imported = importVaultUri(uri, showToast = false)
            if (imported) {
                importedCount++
                if (removeFromSource && tryRemoveSource(uri)) {
                    removedCount++
                } else if (removeFromSource) {
                    failedCount++
                }
            } else {
                failedCount++
            }
        }

        return SharedImportResult(importedCount, removedCount, failedCount)
    }

    private fun importVaultUri(uri: Uri, showToast: Boolean): Boolean {
        if (!isUnlocked) return false
        val name = queryDisplayName(uri) ?: "Imported File"
        val newId = UUID.randomUUID().toString()
        val encryptedName = "$newId.bin"
        val output = File(vaultDir(), encryptedName)

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(output).use { fileOut ->
                val cipher = CryptoUtils.newEncryptCipher()
                fileOut.write(cipher.iv)
                CipherOutputStream(fileOut, cipher).use { cipherOut ->
                    copyStream(input, cipherOut)
                }
            }
        } ?: run {
            Toast.makeText(this, "Unable to read file", Toast.LENGTH_SHORT).show()
            return false
        }

        entries.add(
            VaultFileEntry(
                id = newId,
                originalName = name,
                folder = if (currentFolder == "Trash") PrivateVaultStore.defaultFolder() else currentFolder,
                encryptedName = encryptedName,
                isTrashed = false,
                createdAt = System.currentTimeMillis(),
            )
        )
        persistEntries()
        renderEntries()
        if (showToast) {
            Toast.makeText(this, "File added to vault", Toast.LENGTH_SHORT).show()
        }
        return true
    }

    private fun tryRemoveSource(uri: Uri): Boolean {
        return runCatching {
            if (DocumentsContract.isDocumentUri(this, uri)) {
                DocumentsContract.deleteDocument(contentResolver, uri)
            } else {
                contentResolver.delete(uri, null, null) > 0
            }
        }.getOrDefault(false)
    }

    private fun openEntry(entry: VaultFileEntry) {
        if (entry.isTrashed) {
            Toast.makeText(this, "Restore the file first to open it", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, VaultFileViewerActivity::class.java).apply {
                putExtra(VaultFileViewerActivity.EXTRA_ENTRY_ID, entry.id)
            }
        )
    }

    private fun exportToSystem(entry: VaultFileEntry): Boolean {
        val encryptedFile = File(vaultDir(), entry.encryptedName)
        if (!encryptedFile.exists()) return false

        val fileName = entry.originalName
        val safeFileName = fileName.replace(Regex("""[\\/:*?"<>|]"""), "_")
        val mimeType = mimeTypeFor(fileName)
        return runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/SimpleCalculatorVault")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    values
                ) ?: return@runCatching false
                contentResolver.openOutputStream(uri)?.use { output ->
                    writeDecryptedFile(encryptedFile, output)
                } ?: return@runCatching false
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
                true
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloadsDir.mkdirs()
                val outputFile = File(downloadsDir, safeFileName)
                FileOutputStream(outputFile).use { output ->
                    writeDecryptedFile(encryptedFile, output)
                }
                runCatching {
                    MediaScannerConnection.scanFile(
                        this,
                        arrayOf(outputFile.absolutePath),
                        arrayOf(mimeType),
                        null
                    )
                }
                true
            }
        }.getOrDefault(false)
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && it.moveToFirst()) {
                return it.getString(index)
            }
        }
        return null
    }

    private fun persistEntries() {
        PrivateVaultStore.saveEntries(this, entries)
    }

    private fun vaultDir(): File {
        return File(filesDir, "vault").apply { mkdirs() }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            output.write(buffer, 0, read)
        }
        output.flush()
    }

    private fun writeDecryptedFile(encryptedFile: File, output: OutputStream) {
        FileInputStream(encryptedFile).use { fileIn ->
            val iv = ByteArray(12)
            val readIv = fileIn.read(iv)
            if (readIv != 12) error("Invalid encrypted file")
            val cipher = CryptoUtils.newDecryptCipher(iv)
            CipherInputStream(fileIn, cipher).use { cipherIn ->
                copyStream(cipherIn, output)
            }
        }
    }

    private fun mimeTypeFor(name: String): String {
        val extension = name.substringAfterLast('.', "").lowercase(Locale.getDefault())
        if (extension.isBlank()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }
}
