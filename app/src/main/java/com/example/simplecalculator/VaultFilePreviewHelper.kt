package com.riteshkatre.simplecalculator

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import javax.crypto.CipherInputStream

object VaultFilePreviewHelper {

    fun decryptToTempFile(context: Context, entry: VaultFileEntry): File? {
        val encryptedFile = File(context.filesDir, "vault/${entry.encryptedName}")
        if (!encryptedFile.exists()) return null

        val safeName = entry.originalName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val tempFile = File(context.cacheDir, "vault_open_${entry.id}_$safeName")

        return runCatching {
            FileInputStream(encryptedFile).use { fileIn ->
                val iv = ByteArray(12)
                val readIv = fileIn.read(iv)
                if (readIv != 12) error("Invalid encrypted file")
                val cipher = CryptoUtils.newDecryptCipher(iv)
                FileOutputStream(tempFile).use { tempOut ->
                    CipherInputStream(fileIn, cipher).use { cipherIn ->
                        copyStream(cipherIn, tempOut)
                    }
                }
            }
            tempFile
        }.getOrNull()
    }

    fun mimeTypeFor(name: String): String {
        val extension = name.substringAfterLast('.', "").lowercase(Locale.getDefault())
        if (extension.isBlank()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }

    fun isImage(mimeType: String): Boolean = mimeType.startsWith("image/")
    fun isVideo(mimeType: String): Boolean = mimeType.startsWith("video/")
    fun isPdf(mimeType: String): Boolean = mimeType == "application/pdf"

    fun isTextLike(mimeType: String): Boolean {
        return mimeType.startsWith("text/") || mimeType in setOf(
            "application/json",
            "application/xml",
            "application/javascript",
            "application/x-www-form-urlencoded",
        )
    }

    fun openExternalIntent(context: Context, file: File, mimeType: String): android.content.Intent {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            output.write(buffer, 0, read)
        }
        output.flush()
    }
}
