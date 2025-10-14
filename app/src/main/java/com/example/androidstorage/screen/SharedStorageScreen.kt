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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.androidstorage.data.FileInfo
import java.io.File
import java.io.OutputStreamWriter

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
                AlertDialog(onDismissRequest = { showContentDialog = false },
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

// Helper functions for MediaStore read operations
fun Context.readViaMediaStore(uri: Uri): String {
    return try {
        contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() ?: "" }
    } catch (e: Exception) {
        Toast.makeText(this, "Read failed: ${e.message}", Toast.LENGTH_SHORT).show()
        ""
    }
}

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

@Composable
fun MediaStoreListItem(
    fileInfo: FileInfo, onDelete: () -> Unit, onEdit: () -> Unit, onView: () -> Unit
) {
    val sizeBytes = fileInfo.fileSize

    val fileSize = when {
        sizeBytes > 1_000_000 -> "${sizeBytes / 1_000_000} MB"
        sizeBytes > 1_000 -> "${sizeBytes / 1_000} KB"
        else -> "$sizeBytes bytes"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onView() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileInfo.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = fileSize, style = MaterialTheme.typography.bodySmall, color = Color.Gray
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // Edit Icon
                IconButton(
                    onClick = onEdit, modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit file",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Delete Icon
                IconButton(
                    onClick = onDelete, modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete file",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }


}


//helper function to get file uri info
fun Context.getMediaFileInfo(uri: Uri): FileInfo {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For API 29+ (Android 10 and above) - Use MediaStore
            val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE
            )

            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                    val size =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                    val mimeType =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
                            ?: ""

                    return FileInfo(fileName = displayName, fileSize = size, mimeType = mimeType)
                }
            }
        } else {
            // For API 28 and below - Use File methods
            val filePath = uri.path
            if (!filePath.isNullOrEmpty()) {
                val file = File(filePath)
                if (file.exists()) {
                    return FileInfo(fileName = file.name, fileSize = file.length(), mimeType = "")
                }
            }
        }
        FileInfo(fileName = "File not found", fileSize = 0, mimeType = "")
    } catch (e: Exception) {
        FileInfo(fileName = "Error: ${e.message}", fileSize = 0, mimeType = "")
    }
}

// to get list of files
fun Context.queryMediaStoreFiles(): List<Uri> {
    val files = mutableListOf<Uri>()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // For API 29 and above, use MediaStore query with RELATIVE_PATH

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.RELATIVE_PATH
        )
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%/DecodeAndroid/%")
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
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetFolder = File(downloadsDir, "DecodeAndroid")
        if (targetFolder.exists()) {
            targetFolder.listFiles()?.forEach { file ->
                // Create a file URI for each file
                files.add(Uri.fromFile(file))
            }
        }
        files
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

// Helper functions for deleting file via Media Store
fun Context.deleteViaMediaStore(name: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(name)
        val pathUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val cursor = contentResolver.query(pathUri, projection, selection, selectionArgs, null)
        var deleteUri = Uri.parse("")
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

// Helper functions for updating via Media Store
fun Context.updateViaMediaStore(name: String, content: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(name)
        val pathUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL) // ✅ Scoped Storage
        val cursor = contentResolver.query(pathUri, projection, selection, selectionArgs, null)

        var updateUri = Uri.parse("")

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