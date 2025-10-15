package com.example.androidstorage.screen

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.androidstorage.data.FileInfo
import com.example.androidstorage.screen.dialogscreen.FileOperationDialog
import com.example.androidstorage.screen.dialogscreen.MediaStoreListItem
import java.io.File
import java.io.OutputStreamWriter
import androidx.core.net.toUri
import com.example.androidstorage.domain.deleteViaMediaStore
import com.example.androidstorage.domain.getMediaFileInfo
import com.example.androidstorage.domain.queryMediaStoreFiles
import com.example.androidstorage.domain.readViaMediaStore
import com.example.androidstorage.domain.saveViaMediaStore
import com.example.androidstorage.domain.saveViaMediaStoreAPI
import com.example.androidstorage.domain.updateViaMediaStore

@Composable
fun SharedStorageScreen(padding: PaddingValues) {
    val context = LocalContext.current
    var mediaFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }

    var showDialog by remember { mutableStateOf(false) }
    var showContentDialog by remember { mutableStateOf(false) }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    var fileContent by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    // Permission states
    var hasPermissions by remember { mutableStateOf(false) }

    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
        if (hasPermissions) {
            mediaFiles = context.queryMediaStoreFiles()
        } else {
            Toast.makeText(context, "\"Permission denied\"", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (permissions.all {
                    ContextCompat.checkSelfPermission(
                        context, it
                    ) == PackageManager.PERMISSION_GRANTED
                }) {
                hasPermissions = true
                mediaFiles = context.queryMediaStoreFiles()
            } else {
                permissionLauncher.launch(permissions)
            }
        } else {
            mediaFiles = context.queryMediaStoreFiles()
        }
    }

    // SAF Launchers
    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            context.saveViaMediaStore(it, fileContent)
            mediaFiles = context.queryMediaStoreFiles()
        }
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            if (context.contentResolver.getType(uri)?.startsWith("text/") == true) {
                fileContent = context.readViaMediaStore(uri)
                showContentDialog = true
            } else {
                Toast.makeText(context, "Selected file is not a text file", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            // Show media file info in a Toast
            val fileInfo = context.getMediaFileInfo(uri)
            Toast.makeText(
                context,
                "File Info:\nName: ${fileInfo.fileName}\nSize: ${fileInfo.fileSize / 1024} KB\nType: ${fileInfo.mimeType}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = {
            showDialog = true
            fileContent = ""
            selectedUri = null
        }) {
            Icon(Icons.Default.Add, "Add file")
        }
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(innerPadding)
        ) {
            // Content Display Dialog
            if (showContentDialog) {
                AlertDialog(
                    onDismissRequest = { showContentDialog = false },
                    title = { Text("File Content") },
                    text = { Text(fileContent) },
                    confirmButton = {
                        Button(onClick = { showContentDialog = false }) {
                            Text("Close")
                        }
                    })
            }

            // Buttons for Open File and Pick Media
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { openFileLauncher.launch(arrayOf("text/plain")) }) {
                    Text("Open File")
                }
                Button(onClick = {
                    val mediaType: ActivityResultContracts.PickVisualMedia.VisualMediaType =
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    val request: PickVisualMediaRequest =
                        PickVisualMediaRequest.Builder().setMediaType(mediaType).build()

                    imagePicker.launch(request)
                }) {
                    Text("Pick Media")
                }
            }

            // File List
            LazyColumn {
                items(mediaFiles) { uri ->
                    val fileInfo = context.getMediaFileInfo(uri)
                    MediaStoreListItem(fileInfo = fileInfo, onDelete = {
                        context.deleteViaMediaStore(fileInfo.fileName)
                        mediaFiles = context.queryMediaStoreFiles()
                    }, onEdit = {
                        selectedUri = uri
                        fileContent = context.readViaMediaStore(uri)
                        showDialog = true
                    }, onView = {
                        fileContent = context.readViaMediaStore(uri)
                        showContentDialog = true
                    })
                }
            }
        }
    }

    if (showDialog) {
        val initialName = selectedUri?.let { context.getMediaFileInfo(it) }?.fileName ?: ""
        FileOperationDialog(
            initialName = initialName,
            initialContent = fileContent,
            onDismiss = { showDialog = false },
            onCreateWithSAF = { name, content ->
                fileContent = content
                fileName = name
                createFileLauncher.launch(name)
                showDialog = false
            },
            onCreateWithMediaStore = { name, content ->
                context.saveViaMediaStoreAPI(name, content)
                mediaFiles = context.queryMediaStoreFiles()
                showDialog = false
            },
            onUpdate = { name, content ->
                selectedUri?.let {
                    context.updateViaMediaStore(name, content)
                    mediaFiles = context.queryMediaStoreFiles()
                    showDialog = false
                }
            },
            isEditMode = selectedUri != null
        )
    }
}
