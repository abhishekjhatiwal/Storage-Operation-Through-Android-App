package com.example.androidstorage.data

data class FileInfo(
    val fileName: String, val fileSize: Long, val mimeType: String = ""
)