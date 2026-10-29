package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MaterialPreset
import com.example.model.RenderStyle
import com.example.model.ViewerSettings
import com.example.ui.theme.CadBorder
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadGold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaterialPanel(
    settings: ViewerSettings,
    onRenderStyleSelected: (RenderStyle) -> Unit,
    onMaterialSelected: (MaterialPreset) -> Unit,
    onToggleWireframeOverlay: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleBoundingBox: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0xF50F172A))
            .border(1.dp, CadBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Shading & Material Presets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Simulate jewelry casting metals & 3D print resins",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp).testTag("close_material_panel")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Render Modes
        Text(
            text = "RENDER STYLE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CadCyan
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RenderStyle.values().forEach { style ->
                val isSelected = settings.renderStyle == style
                FilterChip(
                    selected = isSelected,
                    onClick = { onRenderStyleSelected(style) },
                    label = {
                        Text(
                            text = when (style) {
                                RenderStyle.SHADED -> "Shaded"
                                RenderStyle.METALLIC -> "Metallic"
                                RenderStyle.WIREFRAME -> "Wireframe"
                                RenderStyle.TRANSPARENT -> "X-Ray / Ghost"
                                RenderStyle.NORMALS -> "Normals"
                            },
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CadCyan.copy(alpha = 0.25f),
                        selectedLabelColor = CadCyan,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.testTag("style_${style.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Material Presets Grid
        Text(
            text = "MATERIAL PRESETS (${MaterialPreset.ALL_PRESETS.size})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CadGold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(MaterialPreset.ALL_PRESETS) { mat ->
                val isSelected = settings.selectedMaterial.id == mat.id
                MaterialCard(
                    material = mat,
                    isSelected = isSelected,
                    onClick = { onMaterialSelected(mat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Viewport Toggles (Wireframe overlay, Ground grid, Bounding box)
        Text(
            text = "VIEWPORT OVERLAYS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Wireframe Mesh Overlay", color = Color(0xFFE2E8F0), fontSize = 13.sp)
            Switch(
                checked = settings.showWireframeOverlay,
                onCheckedChange = { onToggleWireframeOverlay() },
                colors = SwitchDefaults.colors(checkedThumbColor = CadCyan),
                modifier = Modifier.testTag("toggle_wireframe_overlay")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CAD Ground Grid", color = Color(0xFFE2E8F0), fontSize = 13.sp)
            Switch(
                checked = settings.showGrid,
                onCheckedChange = { onToggleGrid() },
                colors = SwitchDefaults.colors(checkedThumbColor = CadCyan),
                modifier = Modifier.testTag("toggle_grid")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("3D Bounding Box Cage", color = Color(0xFFE2E8F0), fontSize = 13.sp)
            Switch(
                checked = settings.showBoundingBox,
                onCheckedChange = { onToggleBoundingBox() },
                colors = SwitchDefaults.colors(checkedThumbColor = CadGold),
                modifier = Modifier.testTag("toggle_bounding_box")
            )
        }
    }
}

@Composable
private fun MaterialCard(
    material: MaterialPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val swatchColor = Color(
        red = material.diffuseColor[0],
        green = material.diffuseColor[1],
        blue = material.diffuseColor[2]
    )

    Column(
        modifier = Modifier
            .width(115.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CadCyan.copy(alpha = 0.15f) else Color(0xFF1E293B))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) CadCyan else CadBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("material_${material.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = material.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Text(
            text = "${material.densityGCm3} g/cm³",
            color = CadGold,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = material.category,
            color = Color(0xFF94A3B8),
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}
