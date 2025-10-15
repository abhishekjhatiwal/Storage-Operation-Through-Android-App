package com.example.androidstorage.domain

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import com.example.androidstorage.screen.DIRECTORY_NAME
import java.io.File

// Helper functions for updating via Media Store
fun Context.updateViaMediaStore(name: String, content: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(name)
        val pathUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL) //  Scoped Storage
        val cursor = contentResolver.query(pathUri, projection, selection, selectionArgs, null)

        var updateUri = "".toUri()

        cursor?.let {
            while (it.moveToFirst()) {
                val index = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val fileId = it.getLong(index)
                updateUri = ContentUris.withAppendedId(pathUri, fileId)
                break
            }
            it.close()
        }
        contentResolver.openOutputStream(updateUri)?.use {
            it.write(content.toByteArray())
        }
    } else {
        val directory = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY_NAME")
        )
        val file = File(directory, name)
        if (file.exists()) {
            file.writeText(content)
        }
    }
}