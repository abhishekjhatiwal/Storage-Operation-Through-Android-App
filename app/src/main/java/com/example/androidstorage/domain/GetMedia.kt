package com.example.androidstorage.domain

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.androidstorage.data.FileInfo
import java.io.File

//helper function to get file uri info
fun Context.getMediaFileInfo(uri: Uri): FileInfo {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For API 29+ (Android 10 and above) - Use MediaStore
            val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE
            )

            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                    val size =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                    val mimeType =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
                            ?: ""

                    return FileInfo(fileName = displayName, fileSize = size, mimeType = mimeType)
                }
            }
        } else {
            // For API 28 and below - Use File methods
            val filePath = uri.path
            if (!filePath.isNullOrEmpty()) {
                val file = File(filePath)
                if (file.exists()) {
                    return FileInfo(fileName = file.name, fileSize = file.length(), mimeType = "")
                }
            }
        }
        FileInfo(fileName = "File not found", fileSize = 0, mimeType = "")
    } catch (e: Exception) {
        FileInfo(fileName = "Error: ${e.message}", fileSize = 0, mimeType = "")
    }
}