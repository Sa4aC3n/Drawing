package com.art.drawingkids

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Dedicated manager for saving, exporting, gallery browsing, and sharing user drawings.
 * Features:
 * - Scoped Storage compliant (MediaStore on Android 10+ / API 29+)
 * - Robust fallback to public/app directories on Android 9-
 * - Non-zero byte file validation before reporting success
 * - Clean temporary cache management for FileProvider sharing
 * - Integrated gallery retrieval for saved artworks
 */
class DrawingStorageManager(private val context: Context) {

    data class SaveResult(
        val success: Boolean,
        val uri: Uri? = null,
        val fileSizeBytes: Long = 0,
        val errorMessage: String? = null
    )

    data class ShareResult(
        val success: Boolean,
        val shareIntent: Intent? = null,
        val errorMessage: String? = null
    )

    companion object {
        const val SUB_FOLDER_NAME = "DrawingForKids"
        const val MIME_TYPE_JPEG = "image/jpeg"
        const val JPEG_COMPRESSION_QUALITY = 92
        const val SHARE_CACHE_DIR = "share_cache"
        const val SHARE_FILE_NAME = "drawing_share.jpg"

        fun generateFileName(categoryKey: String): String {
            val sanitized = categoryKey.replace("[^a-zA-Z0-9_]".toRegex(), "")
            return "Drawing_${sanitized}_${System.currentTimeMillis()}.jpg"
        }
    }

    /**
     * Saves the provided Bitmap directly to device gallery.
     * Verifies that the file exists and is strictly > 0 bytes.
     */
    fun saveDrawingToGallery(bitmap: Bitmap, categoryKey: String): SaveResult {
        val fileName = generateFileName(categoryKey)

        // Also save a local app copy so the in-app "My Artworks" gallery can always load it on any Android version
        saveToInternalFallback(bitmap, fileName)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(bitmap, fileName)
        } else {
            saveViaLegacyFileSystem(bitmap, fileName)
        }
    }

    private fun saveViaMediaStore(bitmap: Bitmap, fileName: String): SaveResult {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE_JPEG)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$SUB_FOLDER_NAME")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        var insertedUri: Uri? = null
        try {
            insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return SaveResult(success = false, errorMessage = "Failed to create MediaStore entry")

            resolver.openOutputStream(insertedUri)?.use { stream ->
                val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, stream)
                stream.flush()
                if (!compressed) {
                    throw IllegalStateException("Bitmap compression failed")
                }
            } ?: return SaveResult(success = false, errorMessage = "Could not open output stream")

            // Finish pending status
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(insertedUri, contentValues, null, null)

            // Validate file size via asset file descriptor
            var size: Long = 0
            resolver.openAssetFileDescriptor(insertedUri, "r")?.use { afd ->
                size = afd.length
            }

            if (size > 0) {
                SaveResult(success = true, uri = insertedUri, fileSizeBytes = size)
            } else {
                resolver.delete(insertedUri, null, null)
                SaveResult(success = false, errorMessage = "Saved file is empty")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            insertedUri?.let { resolver.delete(it, null, null) }
            SaveResult(success = false, errorMessage = e.localizedMessage)
        }
    }

    private fun saveViaLegacyFileSystem(bitmap: Bitmap, fileName: String): SaveResult {
        return try {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, SUB_FOLDER_NAME).apply { mkdirs() }
            val file = File(appDir, fileName)

            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, stream)
                stream.flush()
            }

            if (file.exists() && file.length() > 0) {
                val uri = Uri.fromFile(file)
                val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                    data = uri
                }
                context.sendBroadcast(scanIntent)
                SaveResult(success = true, uri = uri, fileSizeBytes = file.length())
            } else {
                saveToInternalFallback(bitmap, fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            saveToInternalFallback(bitmap, fileName)
        }
    }

    private fun saveToInternalFallback(bitmap: Bitmap, fileName: String): SaveResult {
        return try {
            val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            val file = File(fallbackDir, fileName)
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, stream)
                stream.flush()
            }
            if (file.exists() && file.length() > 0) {
                SaveResult(success = true, uri = Uri.fromFile(file), fileSizeBytes = file.length())
            } else {
                SaveResult(success = false, errorMessage = "Fallback storage write failed")
            }
        } catch (e: Exception) {
            SaveResult(success = false, errorMessage = e.localizedMessage)
        }
    }

    /**
     * Retrieves all saved drawings created by the child.
     */
    fun getSavedDrawings(): List<File> {
        val results = mutableListOf<File>()

        // 1. Check app-specific external files dir
        val appFolder = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        appFolder?.listFiles { file -> file.extension.equals("jpg", ignoreCase = true) }?.let {
            results.addAll(it)
        }

        // 2. Check public pictures dir
        val publicFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            SUB_FOLDER_NAME
        )
        if (publicFolder.exists()) {
            publicFolder.listFiles { file -> file.extension.equals("jpg", ignoreCase = true) }?.let {
                results.addAll(it)
            }
        }

        return results.distinctBy { it.name }.sortedByDescending { it.lastModified() }
    }

    fun deleteDrawing(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Writes the bitmap to the FileProvider cache and creates an ACTION_SEND Intent.
     */
    fun prepareShareIntent(bitmap: Bitmap, shareChooserTitle: String): ShareResult {
        return try {
            val cacheFolder = File(context.cacheDir, SHARE_CACHE_DIR).apply { mkdirs() }
            val shareFile = File(cacheFolder, SHARE_FILE_NAME)

            FileOutputStream(shareFile).use { fOut ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, fOut)
                fOut.flush()
            }

            if (!shareFile.exists() || shareFile.length() == 0L) {
                return ShareResult(success = false, errorMessage = "Generated share file is empty")
            }

            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                shareFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE_JPEG
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, shareChooserTitle)
            ShareResult(success = true, shareIntent = chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            ShareResult(success = false, errorMessage = e.localizedMessage)
        }
    }

    fun prepareShareIntentForFile(file: File, shareChooserTitle: String): ShareResult {
        return try {
            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE_JPEG
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, shareChooserTitle)
            ShareResult(success = true, shareIntent = chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            ShareResult(success = false, errorMessage = e.localizedMessage)
        }
    }
}
