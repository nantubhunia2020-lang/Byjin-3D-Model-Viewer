package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BottomPanelTab
import com.example.ui.theme.CadBorder
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadGold

@Composable
fun BottomDock(
    activeTab: BottomPanelTab?,
    onTabSelected: (BottomPanelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xF00B0F19), // Dark CAD Dock
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CadBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(
                label = "Material",
                icon = Icons.Default.Palette,
                isSelected = activeTab == BottomPanelTab.MATERIALS,
                badgeColor = CadCyan,
                onClick = { onTabSelected(BottomPanelTab.MATERIALS) }
            )

            DockItem(
                label = "Measure",
                icon = Icons.Default.Straighten,
                isSelected = activeTab == BottomPanelTab.MEASUREMENTS,
                badgeColor = CadCyan,
                onClick = { onTabSelected(BottomPanelTab.MEASUREMENTS) }
            )

            DockItem(
                label = "Weight",
                icon = Icons.Default.Scale,
                isSelected = activeTab == BottomPanelTab.WEIGHT_CALCULATOR,
                badgeColor = CadGold,
                onClick = { onTabSelected(BottomPanelTab.WEIGHT_CALCULATOR) }
            )

            DockItem(
                label = "Section",
                icon = Icons.Default.ContentCut,
                isSelected = activeTab == BottomPanelTab.CROSS_SECTION,
                badgeColor = CadCyan,
                onClick = { onTabSelected(BottomPanelTab.CROSS_SECTION) }
            )

            DockItem(
                label = "Inspect",
                icon = Icons.Default.Info,
                isSelected = activeTab == BottomPanelTab.INSPECT,
                badgeColor = CadCyan,
                onClick = { onTabSelected(BottomPanelTab.INSPECT) }
            )
        }
    }
}

@Composable
private fun DockItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    badgeColor: Color,
    onClick: () -> Unit
) {
    val tag = "dock_tab_${label.lowercase()}"
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) badgeColor.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isSelected) badgeColor.copy(alpha = 0.25f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) badgeColor else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFF94A3B8)
        )
    }
}
