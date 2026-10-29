package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.BoundingBox
import com.example.model.CadViewAngle
import com.example.ui.theme.CadBorder
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadGold

@Composable
fun CadViewCube(
    onSnapView: (CadViewAngle) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC0F172A))
            .border(1.dp, CadBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ViewSnapButton(text = "ISO", onClick = { onSnapView(CadViewAngle.ISOMETRIC) })
        ViewSnapButton(text = "TOP", onClick = { onSnapView(CadViewAngle.TOP) })
        ViewSnapButton(text = "FRONT", onClick = { onSnapView(CadViewAngle.FRONT) })
        ViewSnapButton(text = "RIGHT", onClick = { onSnapView(CadViewAngle.RIGHT) })
    }
}

@Composable
private fun ViewSnapButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B))
            .clickable(onClick = onClick)
            .testTag("snap_view_${text.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFFE2E8F0),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun QuickDimensionsBadge(
    bbox: BoundingBox?,
    scaleFactor: Float,
    modifier: Modifier = Modifier
) {
    if (bbox == null) return

    val sx = bbox.sizeX * scaleFactor
    val sy = bbox.sizeY * scaleFactor
    val sz = bbox.sizeZ * scaleFactor

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xB30F172A))
            .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DimItem(axis = "X", valueMm = sx, color = CadCyan)
            DimItem(axis = "Y", valueMm = sy, color = CadGold)
            DimItem(axis = "Z", valueMm = sz, color = Color(0xFF34D399))
        }
    }
}

@Composable
private fun DimItem(axis: String, valueMm: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$axis: ",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = String.format("%.1f mm", valueMm),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
