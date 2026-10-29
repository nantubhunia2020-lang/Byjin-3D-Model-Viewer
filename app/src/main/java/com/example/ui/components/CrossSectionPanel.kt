package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.model.CrossSectionAxis
import com.example.model.ViewerSettings
import com.example.ui.theme.CadBorder
import com.example.ui.theme.CadCyan

@Composable
fun CrossSectionPanel(
    settings: ViewerSettings,
    onToggleCrossSection: (Boolean) -> Unit,
    onAxisSelected: (CrossSectionAxis) -> Unit,
    onProgressChange: (Float) -> Unit,
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
                    text = "Cross-Section Slicing",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Slice model dynamically to inspect internal walls & cavities",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp).testTag("close_section_panel")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Enable / Disable Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, CadBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Enable Slicing Plane",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (settings.isCrossSectionEnabled) "Active" else "Disabled",
                    color = if (settings.isCrossSectionEnabled) CadCyan else Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = settings.isCrossSectionEnabled,
                onCheckedChange = onToggleCrossSection,
                colors = SwitchDefaults.colors(checkedThumbColor = CadCyan),
                modifier = Modifier.testTag("toggle_cross_section")
            )
        }

        if (settings.isCrossSectionEnabled) {
            Spacer(modifier = Modifier.height(14.dp))

            // Axis Selector
            Text(
                text = "SLICING AXIS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CadCyan
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CrossSectionAxis.values().forEach { axis ->
                    val isSelected = settings.crossSectionAxis == axis
                    val label = when (axis) {
                        CrossSectionAxis.Z_AXIS -> "Z (Height)"
                        CrossSectionAxis.Y_AXIS -> "Y (Depth)"
                        CrossSectionAxis.X_AXIS -> "X (Width)"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onAxisSelected(axis) },
                        label = {
                            Text(
                                text = label,
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
                        modifier = Modifier.testTag("axis_${axis.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Slice Plane Position Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CUT PLANE POSITION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "${(settings.crossSectionProgress * 100).toInt()}%",
                    color = CadCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = settings.crossSectionProgress,
                onValueChange = onProgressChange,
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = CadCyan,
                    activeTrackColor = CadCyan
                ),
                modifier = Modifier.testTag("section_slider")
            )
        }
    }
}
