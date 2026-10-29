package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.gl.ModelGLSurfaceView
import com.example.model.CadViewAngle
import com.example.ui.components.BottomDock
import com.example.ui.components.CadViewCube
import com.example.ui.components.CrossSectionPanel
import com.example.ui.components.MaterialPanel
import com.example.ui.components.MeasurementPanel
import com.example.ui.components.ModelInfoPanel
import com.example.ui.components.QuickDimensionsBadge
import com.example.ui.components.TopBarSection
import com.example.ui.components.WeightCalculatorPanel
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadRose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ViewerScreen(
    viewModel: ModelViewerViewModel,
    onOpenFilePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var surfaceViewRef by remember { mutableStateOf<ModelGLSurfaceView?>(null) }

    // Intercept back button when sub-panel is open
    BackHandler(enabled = uiState.activeBottomPanel != null) {
        viewModel.closeBottomPanel()
    }

    // Sync model & settings with renderer
    LaunchedEffect(uiState.currentModel) {
        surfaceViewRef?.renderer?.currentModel = uiState.currentModel
        surfaceViewRef?.requestRender()
    }

    LaunchedEffect(uiState.settings) {
        surfaceViewRef?.renderer?.settings = uiState.settings
        surfaceViewRef?.requestRender()
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF090D16))) {
        // 1. OpenGL ES 2.0 3D Viewport
        AndroidView(
            factory = { ctx ->
                ModelGLSurfaceView(ctx).also { view ->
                    view.renderer.currentModel = uiState.currentModel
                    view.renderer.settings = uiState.settings
                    surfaceViewRef = view
                }
            },
            update = { view ->
                view.renderer.currentModel = uiState.currentModel
                view.renderer.settings = uiState.settings
            },
            modifier = Modifier.fillMaxSize().testTag("gl_surface_view")
        )

        // 2. Floating Top Header
        TopBarSection(
            model = uiState.currentModel,
            isTurntableActive = uiState.settings.isTurntableActive,
            samples = uiState.sampleList,
            selectedSampleId = uiState.selectedSampleId,
            onOpenFileClick = onOpenFilePicker,
            onSelectSample = { sampleId -> viewModel.loadSample(sampleId) },
            onResetCameraClick = {
                uiState.currentModel?.let { m ->
                    surfaceViewRef?.renderer?.resetCameraToModel(m)
                    surfaceViewRef?.requestRender()
                }
            },
            onToggleTurntable = { viewModel.toggleTurntable() },
            onSnapshotClick = {
                surfaceViewRef?.renderer?.takeSnapshot { bitmap ->
                    scope.launch {
                        shareSnapshot(context, bitmap)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // 3. Quick Dimensions Badge (Top Left Overlay)
        QuickDimensionsBadge(
            bbox = uiState.currentModel?.boundingBox,
            scaleFactor = uiState.settings.scaleFactor,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 70.dp, start = 16.dp)
        )

        // 4. CAD View Angle Cube (Top Right Overlay)
        CadViewCube(
            onSnapView = { angle ->
                surfaceViewRef?.renderer?.snapView(angle)
                surfaceViewRef?.requestRender()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 70.dp, end = 16.dp)
        )

        // 5. Loading Indicator Overlay
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xE60F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, CadCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = CadCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = uiState.loadingMessage,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 6. Error Banner
        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 80.dp, start = 20.dp, end = 20.dp)
        ) {
            uiState.errorMessage?.let { error ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xF0450A0A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CadRose)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Error", tint = CadRose, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = error, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 7. Expandable Active Tool Panel (Positioned right above the dock)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = uiState.activeBottomPanel != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                when (uiState.activeBottomPanel) {
                    BottomPanelTab.MATERIALS -> {
                        MaterialPanel(
                            settings = uiState.settings,
                            onRenderStyleSelected = { viewModel.setRenderStyle(it) },
                            onMaterialSelected = { viewModel.selectMaterial(it) },
                            onToggleWireframeOverlay = { viewModel.toggleWireframeOverlay() },
                            onToggleGrid = { viewModel.toggleGrid() },
                            onToggleBoundingBox = { viewModel.toggleBoundingBox() },
                            onClose = { viewModel.closeBottomPanel() }
                        )
                    }
                    BottomPanelTab.MEASUREMENTS -> {
                        MeasurementPanel(
                            model = uiState.currentModel,
                            scaleFactor = uiState.settings.scaleFactor,
                            onScaleChange = { viewModel.setScaleFactor(it) },
                            onClose = { viewModel.closeBottomPanel() }
                        )
                    }
                    BottomPanelTab.WEIGHT_CALCULATOR -> {
                        WeightCalculatorPanel(
                            model = uiState.currentModel,
                            selectedMaterial = uiState.settings.selectedMaterial,
                            scaleFactor = uiState.settings.scaleFactor,
                            customRateUsd = uiState.settings.customMaterialRateUsd,
                            onRateChange = { viewModel.setCustomRate(it) },
                            onClose = { viewModel.closeBottomPanel() }
                        )
                    }
                    BottomPanelTab.CROSS_SECTION -> {
                        CrossSectionPanel(
                            settings = uiState.settings,
                            onToggleCrossSection = { viewModel.toggleCrossSection(it) },
                            onAxisSelected = { viewModel.setCrossSectionAxis(it) },
                            onProgressChange = { viewModel.setCrossSectionProgress(it) },
                            onClose = { viewModel.closeBottomPanel() }
                        )
                    }
                    BottomPanelTab.INSPECT -> {
                        ModelInfoPanel(
                            model = uiState.currentModel,
                            onClose = { viewModel.closeBottomPanel() }
                        )
                    }
                    null -> {}
                }
            }

            // 8. Bottom Navigation Dock
            BottomDock(
                activeTab = uiState.activeBottomPanel,
                onTabSelected = { tab -> viewModel.toggleBottomPanel(tab) }
            )
        }
    }
}

private suspend fun shareSnapshot(context: Context, bitmap: Bitmap) {
    withContext(Dispatchers.IO) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "byjin_3d_render_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "3D Model Render - Byjin Viewer")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(shareIntent, "Share 3D Model Render"))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Could not share snapshot: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
