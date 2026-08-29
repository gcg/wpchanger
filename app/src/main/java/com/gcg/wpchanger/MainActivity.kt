package com.gcg.wpchanger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.gcg.wpchanger.ui.HomeScreen
import com.gcg.wpchanger.ui.WallpaperViewModel
import com.gcg.wpchanger.ui.theme.WPChangerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WPChangerTheme {
                val pickMediaLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickMultipleVisualMedia(),
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        viewModel.importWallpapers(uris)
                    }
                }

                val openFolderLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree(),
                ) { treeUri ->
                    treeUri?.let { uri ->
                        viewModel.importFolder(uri)
                    }
                }

                HomeScreen(
                    viewModel = viewModel,
                    onPickPhotosClick = {
                        pickMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onPickFolderClick = {
                        openFolderLauncher.launch(null)
                    },
                )
            }
        }
    }
}
