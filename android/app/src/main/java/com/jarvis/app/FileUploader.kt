package com.jarvis.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * File picker and uploader for Android
 * Handles file selection, reading, and uploading to the backend
 */
class FileUploader(private val context: Context) {

    interface Callback {
        fun onSuccess(fileName: String, fileSize: Long, content: String)
        fun onError(error: String)
    }

    private var currentCallback: Callback? = null

    fun pickFile(activity: Activity, callback: Callback) {
        currentCallback = callback

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/*",
                "application/pdf",
                "application/json",
                "application/csv",
                "image/*",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel"
            ))
        }

        activity.startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != FILE_PICKER_REQUEST_CODE || resultCode != Activity.RESULT_OK) {
            return false
        }

        val uri = data?.data ?: run {
            currentCallback?.onError("Aucun fichier sélectionné")
            return true
        }

        processFile(uri)
        return true
    }

    private fun processFile(uri: Uri) {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)

        val fileName: String
        val fileSize: Long

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

                fileName = if (nameIndex != -1) it.getString(nameIndex) else "unknown"
                fileSize = if (sizeIndex != -1) it.getLong(sizeIndex) else 0
            } else {
                currentCallback?.onError("Impossible de lire le fichier")
                cursor.close()
                return
            }
        } ?: run {
            currentCallback?.onError("Fichier non accessible")
            return
        }

        // Check file size (max 10MB)
        if (fileSize > 10 * 1024 * 1024) {
            currentCallback?.onError("Fichier trop volumineux (${formatSize(fileSize)}). Maximum: 10 MB")
            return
        }

        // Read file content
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val content = readInputStream(inputStream, fileName)
            inputStream?.close()

            currentCallback?.onSuccess(fileName, fileSize, content)
        } catch (e: Exception) {
            currentCallback?.onError("Erreur de lecture: ${e.message}")
        }
    }

    private fun readInputStream(inputStream: InputStream?, fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()

        return when (extension) {
            "pdf" -> "[Fichier PDF - nécessite une analyse backend]"
            "csv", "json", "txt", "md", "py", "js", "html", "css", "xml" -> {
                inputStream?.bufferedReader()?.use { it.readText() }
                    ?: "[Impossible de lire le contenu]"
            }
            else -> "[Fichier binaire - analyse par vision AI requise]"
        }
    }

    fun uploadFile(fileName: String, fileContent: String, backendUrl: String, callback: (Boolean, String) -> Unit) {
        val client = OkHttpClient()

        // Create temporary file
        val tempFile = File(context.cacheDir, fileName)
        try {
            tempFile.writeText(fileContent)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(backendUrl)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback(false, "Erreur réseau: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        callback(true, body)
                    } else {
                        callback(false, "Erreur serveur: ${response.code}")
                    }
                }
            })
        } catch (e: Exception) {
            callback(false, "Erreur: ${e.message}")
        } finally {
            tempFile.delete()
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        }
    }

    companion object {
        const val FILE_PICKER_REQUEST_CODE = 1001
    }
}
