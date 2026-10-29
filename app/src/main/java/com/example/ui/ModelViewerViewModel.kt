package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CrossSectionAxis
import com.example.model.MaterialPreset
import com.example.model.MeshModel
import com.example.model.RenderStyle
import com.example.model.ViewerSettings
import com.example.parser.ObjParser
import com.example.parser.Rhino3dmParser
import com.example.parser.SampleModels
import com.example.parser.StlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BottomPanelTab {
    MATERIALS,
    MEASUREMENTS,
    WEIGHT_CALCULATOR,
    CROSS_SECTION,
    INSPECT
}

data class ViewerUiState(
    val currentModel: MeshModel? = null,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val errorMessage: String? = null,
    val settings: ViewerSettings = ViewerSettings(),
    val activeBottomPanel: BottomPanelTab? = null,
    val sampleList: List<SampleModels.SampleInfo> = SampleModels.ALL_SAMPLES,
    val selectedSampleId: String = "solitaire_ring"
)

class ModelViewerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    init {
        // Load default high quality CAD sample model on launch
        loadSample("solitaire_ring")
    }

    fun loadSample(sampleId: String) {
        val sample = SampleModels.ALL_SAMPLES.find { it.id == sampleId } ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Generating ${sample.title}...") }
            try {
                val model = withContext(Dispatchers.Default) {
                    sample.createModel()
                }
                _uiState.update {
                    it.copy(
                        currentModel = model,
                        isLoading = false,
                        selectedSampleId = sampleId,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load sample: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun loadFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Reading 3D file...") }
            try {
                val (fileName, fileSizeBytes) = getFileInfo(context, uri)
                val model = withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Cannot open input stream for $uri")
                    inputStream.use { stream ->
                        val lower = fileName.lowercase()
                        when {
                            lower.endsWith(".stl") -> StlParser.parse(stream, fileName, fileSizeBytes)
                            lower.endsWith(".3dm") -> Rhino3dmParser.parse(stream, fileName, fileSizeBytes)
                            lower.endsWith(".obj") -> ObjParser.parse(stream, fileName, fileSizeBytes)
                            else -> {
                                // Probe content: try STL first, then Rhino, then OBJ
                                val bytes = stream.readBytes()
                                val probeText = String(bytes, 0, minOf(80, bytes.size), Charsets.US_ASCII)
                                if (probeText.contains("3D Model", ignoreCase = true) ||
                                    probeText.contains("Rhino", ignoreCase = true)) {
                                    Rhino3dmParser.parse(bytes.inputStream(), fileName, fileSizeBytes)
                                } else {
                                    StlParser.parse(bytes.inputStream(), fileName, fileSizeBytes)
                                }
                            }
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        currentModel = model,
                        isLoading = false,
                        errorMessage = null,
                        selectedSampleId = "" // Custom uploaded model
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not parse model: ${e.localizedMessage ?: "Invalid or corrupted 3D format"}"
                    )
                }
            }
        }
    }

    private fun getFileInfo(context: Context, uri: Uri): Pair<String, Long> {
        var name = "Model.stl"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) { }
        return Pair(name, size)
    }

    fun toggleBottomPanel(tab: BottomPanelTab) {
        _uiState.update {
            val newTab = if (it.activeBottomPanel == tab) null else tab
            it.copy(activeBottomPanel = newTab)
        }
    }

    fun closeBottomPanel() {
        _uiState.update { it.copy(activeBottomPanel = null) }
    }

    fun setRenderStyle(style: RenderStyle) {
        _uiState.update { it.copy(settings = it.settings.copy(renderStyle = style)) }
    }

    fun selectMaterial(material: MaterialPreset) {
        _uiState.update {
            it.copy(
                settings = it.settings.copy(
                    selectedMaterial = material,
                    renderStyle = if (material.isMetallic) RenderStyle.METALLIC else RenderStyle.SHADED
                )
            )
        }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(settings = it.settings.copy(showGrid = !it.settings.showGrid)) }
    }

    fun toggleBoundingBox() {
        _uiState.update { it.copy(settings = it.settings.copy(showBoundingBox = !it.settings.showBoundingBox)) }
    }

    fun toggleWireframeOverlay() {
        _uiState.update { it.copy(settings = it.settings.copy(showWireframeOverlay = !it.settings.showWireframeOverlay)) }
    }

    fun toggleTurntable() {
        _uiState.update { it.copy(settings = it.settings.copy(isTurntableActive = !it.settings.isTurntableActive)) }
    }

    fun setScaleFactor(scale: Float) {
        _uiState.update { it.copy(settings = it.settings.copy(scaleFactor = scale.coerceIn(0.1f, 5.0f))) }
    }

    fun toggleCrossSection(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(isCrossSectionEnabled = enabled)) }
    }

    fun setCrossSectionAxis(axis: CrossSectionAxis) {
        _uiState.update { it.copy(settings = it.settings.copy(crossSectionAxis = axis)) }
    }

    fun setCrossSectionProgress(progress: Float) {
        _uiState.update { it.copy(settings = it.settings.copy(crossSectionProgress = progress.coerceIn(0.0f, 1.0f))) }
    }

    fun setCustomRate(rate: Double?) {
        _uiState.update { it.copy(settings = it.settings.copy(customMaterialRateUsd = rate)) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
