package com.example.androidstorage.domain

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

// to get list of files
fun Context.queryMediaStoreFiles(): List<Uri> {
    val files = mutableListOf<Uri>()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // For API 29 and above, use MediaStore query with RELATIVE_PATH

        val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.RELATIVE_PATH)
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%/AbhishekJhatiwal/%")
        val pathUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

        this.contentResolver.query(
            pathUri, projection, selection, selectionArgs, null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                files.add(ContentUris.withAppendedId(pathUri, id))
            }
        }
        files
    } else {
        // For API 28 and below, fall back to using the File API
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetFolder = File(downloadsDir, "AbhishekJhatiwal")
        if (targetFolder.exists()) {
            targetFolder.listFiles()?.forEach { file ->
                // Create a file URI for each file
                files.add(Uri.fromFile(file))
            }
        }
        files
    }
}
