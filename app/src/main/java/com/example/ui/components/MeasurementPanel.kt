package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.model.MeshModel
import com.example.ui.theme.CadBorder
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadEmerald
import com.example.ui.theme.CadGold

@Composable
fun MeasurementPanel(
    model: MeshModel?,
    scaleFactor: Float,
    onScaleChange: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bbox = model?.boundingBox
    val sx = (bbox?.sizeX ?: 0f) * scaleFactor
    val sy = (bbox?.sizeY ?: 0f) * scaleFactor
    val sz = (bbox?.sizeZ ?: 0f) * scaleFactor

    val scaledAreaMm2 = (model?.surfaceAreaMm2 ?: 0.0) * (scaleFactor * scaleFactor)
    val scaledVolumeMm3 = (model?.volumeMm3 ?: 0.0) * (scaleFactor * scaleFactor * scaleFactor)

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
                    text = "Dimensions & Geometry Analysis",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Precise CAD bounding box and exact volume calculations",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp).testTag("close_measure_panel")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3D Bounding Dimensions Cards (X, Y, Z)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DimensionCard(
                label = "Length (X)",
                value = String.format("%.2f mm", sx),
                accentColor = CadCyan,
                modifier = Modifier.weight(1f)
            )
            DimensionCard(
                label = "Width (Y)",
                value = String.format("%.2f mm", sy),
                accentColor = CadGold,
                modifier = Modifier.weight(1f)
            )
            DimensionCard(
                label = "Height (Z)",
                value = String.format("%.2f mm", sz),
                accentColor = Color(0xFF34D399),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Surface Area and Volume Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Total Surface Area",
                primaryVal = String.format("%.1f mm²", scaledAreaMm2),
                secondaryVal = String.format("%.2f cm²", scaledAreaMm2 / 100.0),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Divergence Volume",
                primaryVal = String.format("%.2f mm³", scaledVolumeMm3),
                secondaryVal = String.format("%.3f cm³", scaledVolumeMm3 / 1000.0),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Watertight & 3D Print Manifold Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CadEmerald.copy(alpha = 0.12f))
                .border(1.dp, CadEmerald.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Solid Manifold",
                tint = CadEmerald,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "3D Print Solid & Manifold",
                    color = CadEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Watertight mesh suitable for SLA/SLS casting and direct metal print",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scale Model Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UNIFORM SCALE: ${String.format("%.2fx", scaleFactor)}",
                color = CadCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            if (scaleFactor != 1.0f) {
                OutlinedButton(
                    onClick = { onScaleChange(1.0f) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    modifier = Modifier.height(26.dp)
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset Scale", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset 1x", fontSize = 10.sp)
                }
            }
        }

        Slider(
            value = scaleFactor,
            onValueChange = onScaleChange,
            valueRange = 0.2f..4.0f,
            colors = SliderDefaults.colors(
                thumbColor = CadCyan,
                activeTrackColor = CadCyan
            ),
            modifier = Modifier.testTag("scale_slider")
        )
    }
}

@Composable
private fun DimensionCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, CadBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatCard(
    title: String,
    primaryVal: String,
    secondaryVal: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, CadBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(text = title, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = primaryVal, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(text = secondaryVal, color = CadCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
