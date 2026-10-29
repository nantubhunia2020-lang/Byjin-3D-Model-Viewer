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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.CadGold

@Composable
fun ModelInfoPanel(
    model: MeshModel?,
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
                    text = "CAD Model Diagnostics",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Geometry statistics and file metadata",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp).testTag("close_info_panel")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (model != null) {
            InfoRow(label = "File Name", value = model.name)
            InfoRow(label = "Format", value = model.format, valueColor = CadGold)
            InfoRow(
                label = "File Size",
                value = if (model.fileSizeBytes > 1024 * 1024) {
                    String.format("%.2f MB", model.fileSizeBytes / (1024.0 * 1024.0))
                } else {
                    String.format("%.1f KB", model.fileSizeBytes / 1024.0)
                }
            )
            InfoRow(label = "Triangles / Faces", value = String.format("%,d", model.triangleCount), valueColor = CadCyan)
            InfoRow(label = "Vertex Count", value = String.format("%,d", model.vertexCount))
            InfoRow(
                label = "Bounding Range X",
                value = String.format("[%.1f, %.1f] mm", model.boundingBox.minX, model.boundingBox.maxX)
            )
            InfoRow(
                label = "Bounding Range Y",
                value = String.format("[%.1f, %.1f] mm", model.boundingBox.minY, model.boundingBox.maxY)
            )
            InfoRow(
                label = "Bounding Range Z",
                value = String.format("[%.1f, %.1f] mm", model.boundingBox.minZ, model.boundingBox.maxZ)
            )
            InfoRow(
                label = "Mesh Memory Buffer",
                value = String.format("%.2f MB", (model.vertexCount * 3 * 4 * 2) / (1024.0 * 1024.0))
            )
        } else {
            Text("No model loaded", color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
