package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MeshModel
import com.example.parser.SampleModels
import com.example.ui.theme.CadBorder
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadGold
import com.example.ui.theme.CadSurfaceVariant

@Composable
fun TopBarSection(
    model: MeshModel?,
    isTurntableActive: Boolean,
    samples: List<SampleModels.SampleInfo>,
    selectedSampleId: String,
    onOpenFileClick: () -> Unit,
    onSelectSample: (String) -> Unit,
    onResetCameraClick: () -> Unit,
    onToggleTurntable: () -> Unit,
    onSnapshotClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var samplesMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xCC0F172A), // Translucent slate
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CadBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Branding & Model Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CadCyan.copy(alpha = 0.15f))
                        .border(1.dp, CadCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "3D Viewer",
                        tint = CadCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = model?.name ?: "Byjin 3D Viewer",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Format Badge
                        model?.let { m ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CadGold.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = m.format,
                                    color = CadGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "• ${(m.triangleCount / 1000f).let { String.format("%.1fk faces", it) }}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        } ?: run {
                            Text(
                                text = "STL & Rhino 3DM Engine",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Quick Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Samples Dropdown
                Box {
                    IconButton(
                        onClick = { samplesMenuExpanded = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("samples_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Sample Models",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = samplesMenuExpanded,
                        onDismissRequest = { samplesMenuExpanded = false },
                        modifier = Modifier
                            .background(Color(0xFF0F172A))
                            .border(1.dp, CadBorder, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "PRELOADED CAD SAMPLES",
                            color = CadCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )

                        samples.forEach { sample ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = sample.title,
                                                color = if (sample.id == selectedSampleId) CadCyan else Color.White,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(CadGold.copy(alpha = 0.2f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = sample.formatBadge,
                                                    color = CadGold,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = sample.subtitle,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                onClick = {
                                    samplesMenuExpanded = false
                                    onSelectSample(sample.id)
                                }
                            )
                        }
                    }
                }

                // Open File Button (SAF Picker)
                IconButton(
                    onClick = onOpenFileClick,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("open_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Open 3D File",
                        tint = CadCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Turntable Auto-Rotate Toggle
                IconButton(
                    onClick = onToggleTurntable,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isTurntableActive) CadCyan.copy(alpha = 0.2f) else Color.Transparent)
                        .testTag("turntable_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = "Turntable 360",
                        tint = if (isTurntableActive) CadCyan else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Reset Camera View
                IconButton(
                    onClick = onResetCameraClick,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("reset_view_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Camera View",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Snapshot / Share
                IconButton(
                    onClick = onSnapshotClick,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("snapshot_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Take Snapshot",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
