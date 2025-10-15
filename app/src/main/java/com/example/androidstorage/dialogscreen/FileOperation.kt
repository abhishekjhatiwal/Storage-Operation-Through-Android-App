package com.example.androidstorage.dialogscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FileOperationDialog(
    initialName: String,
    initialContent: String,
    onDismiss: () -> Unit,
    onCreateWithSAF: (String, String) -> Unit,
    onCreateWithMediaStore: (String, String) -> Unit,
    onUpdate: (String, String) -> Unit,
    isEditMode: Boolean
) {
    var fileName by remember { mutableStateOf(initialName) }
    var content by remember { mutableStateOf(initialContent) }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "Edit File" else "Create File") },
        text = {
            Column {
                TextField(value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File name") },
                    enabled = !isEditMode
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.height(150.dp)
                )
            }
        },
        confirmButton = {
            Column {
                if (isEditMode) {
                    Button(onClick = { onUpdate(fileName, content) }) {
                        Text("Update File")
                    }
                } else {
                    Button(onClick = { onCreateWithSAF(fileName, content) }) {
                        Text("Save via SAF")
                    }
                    Button(onClick = { onCreateWithMediaStore(fileName, content) }) {
                        Text("Save via MediaStore")
                    }
                }
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        })
}