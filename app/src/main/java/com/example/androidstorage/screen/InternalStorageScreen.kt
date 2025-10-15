package com.example.androidstorage.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun InternalStorageScreen(padding: PaddingValues) {

    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var files by remember { mutableStateOf(context.createIntDirectory().listFiles()?.toList() ?: emptyList()) }

    // Refresh file list
    fun loadInternalFiles() {
        files = context.createIntDirectory().listFiles()?.toList() ?: emptyList()
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
                            if (file.delete()) loadInternalFiles()
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
                    //1. one way
                    /*context.openFileOutput(name, Context.MODE_PRIVATE).use {
                        it.write(content.toByteArray())
                    }*/

                    //2. second way
                    val directory = context.createIntDirectory()
                    val fileName = name.replace(".txt", "").plus(".txt")
                    val file = File(directory, fileName)
                    file.writeText(content)

                    loadInternalFiles()
                    showDialog = false
                    selectedFile = null
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

const val DIRECTORY_NAME = "abhishek_jhatiwal"

fun Context.createIntDirectory(): File {
    val directory = filesDir
    val file = File(directory, DIRECTORY_NAME)
    if (!file.exists()) file.mkdir()
    return file
}

// CommonDialog.kt
@Composable
fun FileDialog(
    title: String,
    initialName: String = "",
    initialContent: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, content: String) -> Unit
) {
    var fileName by remember { mutableStateOf(initialName) }
    var content by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File name") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.height(150.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(fileName, content) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}