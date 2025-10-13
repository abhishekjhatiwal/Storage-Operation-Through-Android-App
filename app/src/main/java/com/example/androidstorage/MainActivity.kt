package com.example.androidstorage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.androidstorage.screen.ExternalStorageScreen
import com.example.androidstorage.screen.InternalStorageScreen
import com.example.androidstorage.screen.SharedStorageScreen
import com.example.androidstorage.ui.theme.AndroidStorageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidStorageTheme {
                StorageApp()
            }
        }
    }
}

@Composable
fun StorageApp() {
    val tabs = listOf("Internal", "External/Scoped", "Shared")
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> InternalStorageScreen(padding)
            1 -> ExternalStorageScreen(padding)
            2 -> SharedStorageScreen(padding)
        }
    }
}

