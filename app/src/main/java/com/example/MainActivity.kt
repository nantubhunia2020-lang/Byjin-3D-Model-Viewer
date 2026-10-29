package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.ui.ModelViewerViewModel
import com.example.ui.ViewerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ModelViewerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle file passed via ACTION_VIEW intent (e.g. user taps an STL or 3DM file)
        intent?.data?.let { uri ->
            viewModel.loadFromUri(this, uri)
        }

        setContent {
            MyApplicationTheme(darkTheme = true) {
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    uri?.let {
                        viewModel.loadFromUri(this, it)
                    }
                }

                ViewerScreen(
                    viewModel = viewModel,
                    onOpenFilePicker = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "*/*",
                                "application/sla",
                                "model/stl",
                                "model/3dm",
                                "model/obj"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
            viewModel.loadFromUri(this, uri)
        }
    }
}
