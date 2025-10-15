package com.example.androidstorage.domain

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.androidstorage.screen.DIRECTORY_NAME
import java.io.File
import androidx.core.net.toUri

// Helper functions for deleting file via Media Store
fun Context.deleteViaMediaStore(name: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(name)
        val pathUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val cursor = contentResolver.query(pathUri, projection, selection, selectionArgs, null)
        var deleteUri = "".toUri()
        cursor?.let {
            while (it.moveToFirst()) {
                val index = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val fileId = it.getLong(index)
                deleteUri = ContentUris.withAppendedId(pathUri, fileId)
                break
            }
            it.close()
        }
        contentResolver.delete(deleteUri, null, null)
    } else {
        val directory = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY_NAME")
        )
        val file = File(directory, name)
        if (file.exists()) file.delete()
    }
}