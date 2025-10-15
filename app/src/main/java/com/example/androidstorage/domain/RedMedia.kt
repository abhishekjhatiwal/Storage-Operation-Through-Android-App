package com.example.androidstorage.domain

import android.content.Context
import android.net.Uri
import android.widget.Toast

// Helper functions for MediaStore read operations
fun Context.readViaMediaStore(uri: Uri): String {
    return try {
        contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() ?: "" }
    } catch (e: Exception) {
        Toast.makeText(this, "Read failed: ${e.message}", Toast.LENGTH_SHORT).show()
        ""
    }
}