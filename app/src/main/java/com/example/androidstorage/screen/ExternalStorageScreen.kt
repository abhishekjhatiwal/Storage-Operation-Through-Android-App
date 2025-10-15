package com.example.androidstorage.screen

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.androidstorage.screen.dialogscreen.FileListItem
import java.io.File


@Composable
fun ExternalStorageScreen(padding: PaddingValues) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(emptyList<File>()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<File?>(null) }

    fun loadExternalFiles() {
        context.createExtDir().listFiles()?.let {
            files = it.toList()
        }
    }

    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) loadExternalFiles() else Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        loadExternalFiles()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, "Add file")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(padding).padding(innerPadding)) {
            LazyColumn {
                items(files) { file ->
                    FileListItem(
                        file = file,
                        onDelete = {
                            if (file.delete()) loadExternalFiles()
                        },
                        onEdit = {
                            selectedFile = file
                            showDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        FileDialog(
            title = if (selectedFile == null) "Create File" else "Edit File",
            initialName = selectedFile?.name?.replace(".txt", "") ?: "",
            initialContent = selectedFile?.readText() ?: "",
            onDismiss = {
                showDialog = false
                selectedFile = null
            },
            onConfirm = { name, content ->
                try {
                    val directory = context.createExtDir()
                    val fileName = name.replace(".txt", "").plus(".txt")
                    val file = File(directory, fileName)
                    file.writeText(content)

                    loadExternalFiles()
                    showDialog = false
                    selectedFile = null
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

fun Context.createExtDir(): File {
    val directory = getExternalFilesDir(null)
    val file = File(directory, DIRECTORY_NAME)
    if (file.exists().not()) file.mkdir()
    return file
}