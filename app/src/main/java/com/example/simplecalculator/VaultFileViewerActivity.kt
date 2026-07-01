package com.riteshkatre.simplecalculator

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.media.MediaPlayer
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.riteshkatre.simplecalculator.databinding.ActivityVaultFileViewerBinding
import java.io.File

class VaultFileViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ENTRY_ID = "extra_entry_id"
    }

    private lateinit var binding: ActivityVaultFileViewerBinding
    private var openedFile: File? = null
    private var pdfRenderer: PdfRenderer? = null
    private var pdfPage: PdfRenderer.Page? = null
    private var pdfFileDescriptor: ParcelFileDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityVaultFileViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnOpenExternally.setOnClickListener { openExternally() }

        val entryId = intent.getStringExtra(EXTRA_ENTRY_ID)
        if (entryId.isNullOrBlank()) {
            Toast.makeText(this, "Unable to open file", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val entry = PrivateVaultStore.loadEntries(this).firstOrNull { it.id == entryId }
        if (entry == null || entry.isTrashed) {
            Toast.makeText(this, "File not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.fileTitle.text = entry.originalName
        val mimeType = VaultFilePreviewHelper.mimeTypeFor(entry.originalName)
        binding.fileMime.text = mimeType

        openedFile = VaultFilePreviewHelper.decryptToTempFile(this, entry)
        if (openedFile == null || openedFile?.exists() != true) {
            Toast.makeText(this, "Unable to decrypt file", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showPreview(openedFile!!, mimeType)
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { pdfPage?.close() }
        runCatching { pdfRenderer?.close() }
        runCatching { pdfFileDescriptor?.close() }
        openedFile?.delete()
    }

    private fun showPreview(file: File, mimeType: String) {
        when {
            VaultFilePreviewHelper.isImage(mimeType) -> showImage(file)
            VaultFilePreviewHelper.isVideo(mimeType) -> showVideo(file)
            VaultFilePreviewHelper.isPdf(mimeType) -> showPdf(file)
            VaultFilePreviewHelper.isTextLike(mimeType) -> showText(file)
            else -> showUnsupported(mimeType)
        }
    }

    private fun showImage(file: File) {
        hideAllPreviews()
        binding.imagePreview.visibility = android.view.View.VISIBLE
        loadBitmap(file)?.let {
            binding.imagePreview.setImageBitmap(it)
            binding.btnOpenExternally.visibility = android.view.View.GONE
        } ?: run {
            showUnsupported(VaultFilePreviewHelper.mimeTypeFor(file.name))
        }
    }

    private fun showVideo(file: File) {
        hideAllPreviews()
        binding.videoPreview.visibility = android.view.View.VISIBLE
        val controller = MediaController(this)
        controller.setAnchorView(binding.videoPreview)
        binding.videoPreview.setMediaController(controller)
        binding.videoPreview.setVideoPath(file.absolutePath)
        binding.videoPreview.setOnErrorListener { _, _, _ ->
            showUnsupported(VaultFilePreviewHelper.mimeTypeFor(file.name))
            true
        }
        binding.videoPreview.setOnPreparedListener { player: MediaPlayer ->
            player.isLooping = false
            binding.videoPreview.start()
        }
        binding.btnOpenExternally.visibility = android.view.View.GONE
    }

    private fun showPdf(file: File) {
        hideAllPreviews()
        binding.imagePreview.visibility = android.view.View.VISIBLE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            showUnsupported("application/pdf")
            return
        }

        runCatching {
            pdfFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(pdfFileDescriptor!!)
            pdfPage = pdfRenderer!!.openPage(0)
            val bitmap = Bitmap.createBitmap(
                pdfPage!!.width,
                pdfPage!!.height,
                Bitmap.Config.ARGB_8888
            )
            pdfPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            binding.imagePreview.setImageBitmap(bitmap)
            binding.btnOpenExternally.visibility = android.view.View.VISIBLE
        }.onFailure {
            showUnsupported("application/pdf")
        }
    }

    private fun showText(file: File) {
        hideAllPreviews()
        binding.textScroll.visibility = android.view.View.VISIBLE
        binding.textPreview.text = runCatching {
            file.readText(Charsets.UTF_8)
        }.getOrElse {
            "This file could not be displayed as text.\n\nYou can still open it externally."
        }
        binding.btnOpenExternally.visibility = android.view.View.VISIBLE
    }

    private fun showUnsupported(mimeType: String) {
        hideAllPreviews()
        binding.unsupportedPreview.visibility = android.view.View.VISIBLE
        binding.unsupportedMessage.text = "Preview not available for $mimeType.\nYou can still open it externally."
        binding.btnOpenExternally.visibility = android.view.View.VISIBLE
    }

    private fun hideAllPreviews() {
        binding.imagePreview.visibility = android.view.View.GONE
        binding.videoPreview.visibility = android.view.View.GONE
        binding.textScroll.visibility = android.view.View.GONE
        binding.unsupportedPreview.visibility = android.view.View.GONE
    }

    private fun openExternally() {
        val file = openedFile ?: return
        val mimeType = VaultFilePreviewHelper.mimeTypeFor(binding.fileTitle.text.toString())
        val intent = VaultFilePreviewHelper.openExternalIntent(this, file, mimeType)
        runCatching { startActivity(Intent.createChooser(intent, "Open with")) }
            .onFailure {
                Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadBitmap(file: File): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(file)
            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = false
                decoder.setTargetSize(
                    resources.displayMetrics.widthPixels,
                    resources.displayMetrics.heightPixels
                )
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val targetWidth = resources.displayMetrics.widthPixels
            val targetHeight = resources.displayMetrics.heightPixels
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, targetWidth, targetHeight)
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
        var sampleSize = 1
        if (height > targetHeight || width > targetWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / sampleSize) >= targetHeight && (halfWidth / sampleSize) >= targetWidth) {
                sampleSize *= 2
            }
        }
        return sampleSize.coerceAtLeast(1)
    }
}
