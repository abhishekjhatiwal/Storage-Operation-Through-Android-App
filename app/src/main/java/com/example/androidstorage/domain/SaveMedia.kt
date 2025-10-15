package com.example.androidstorage.domain

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.androidstorage.screen.DIRECTORY_NAME
import java.io.File
import java.io.OutputStreamWriter



// Helper functions for MediaStore write operations
fun Context.saveViaMediaStore(uri: Uri, content: String) {
    try {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
            outputStream.flush()
        }
    } catch (e: Exception) {
        Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Helper functions for storing file via Media Store
fun Context.saveViaMediaStoreAPI(name: String, content: String): Uri? {
    var uri: Uri? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOCUMENTS.plus("/$DIRECTORY_NAME")
            )
        }
        val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        uri = contentResolver.insert(
            contentUri, contentValues
        )

        uri?.let { uri1 ->
            contentResolver.openOutputStream(uri1).use { outputStream ->
                OutputStreamWriter(outputStream).use {
                    it.write(content)
                }
            }
        }

    } else {
        val state = Environment.getExternalStorageState()
        if (state == Environment.MEDIA_MOUNTED) {
            val directory = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY_NAME")
            )
            if (directory.exists().not()) directory.mkdirs()

            val file = File(directory, name)
            file.writeText(content)
        } else {
            Toast.makeText(this, "No External Media Mounted", Toast.LENGTH_SHORT).show()
        }
    }

    return uri
}