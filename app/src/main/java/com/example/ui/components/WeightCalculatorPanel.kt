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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MaterialPreset
import com.example.model.MeshModel
import com.example.ui.theme.CadBorder
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadGold
import com.example.ui.theme.CadGoldBright

@Composable
fun WeightCalculatorPanel(
    model: MeshModel?,
    selectedMaterial: MaterialPreset,
    scaleFactor: Float,
    customRateUsd: Double?,
    onRateChange: (Double?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weightGrams = model?.calculateWeightGrams(selectedMaterial.densityGCm3, scaleFactor) ?: 0.0
    val weightDwt = weightGrams * 0.643015 // pennyweight (dwt)
    val weightCt = weightGrams * 5.0 // carats

    val activeRate = customRateUsd ?: selectedMaterial.defaultPricePerGramUsd
    val estimatedCost = weightGrams * activeRate

    var rateInputText by remember(selectedMaterial.id, customRateUsd) {
        mutableStateOf(String.format("%.2f", activeRate))
    }

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
                    text = "Byjin Weight & Casting Estimator",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Jewelry casting metal weights & 3D print resin cost",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp).testTag("close_weight_panel")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Material Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, CadGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CadGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = CadGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = selectedMaterial.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Density: ${selectedMaterial.densityGCm3} g/cm³  •  ${selectedMaterial.category}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            if (scaleFactor != 1.0f) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CadCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${String.format("%.2f", scaleFactor)}x Scale",
                        color = CadCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Weight Highlights (Grams, DWT, Carats)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeightCard(
                label = "Grams (g)",
                value = String.format("%.2f g", weightGrams),
                subtitle = "Metric Mass",
                highlight = true,
                modifier = Modifier.weight(1.2f)
            )
            WeightCard(
                label = "DWT (Pennyweight)",
                value = String.format("%.2f dwt", weightDwt),
                subtitle = "Jewelry Standard",
                highlight = false,
                modifier = Modifier.weight(1f)
            )
            WeightCard(
                label = "Carats (ct)",
                value = String.format("%.1f ct", weightCt),
                subtitle = "Gem Mass",
                highlight = false,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cost Estimation Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, CadBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ESTIMATED MATERIAL VALUE",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format("$%.2f USD", estimatedCost),
                    color = CadGoldBright,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Based on $weightGrams g @ $${String.format("%.2f", activeRate)}/g",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp
                )
            }

            // Editable Rate Input
            OutlinedTextField(
                value = rateInputText,
                onValueChange = { input ->
                    rateInputText = input
                    val parsed = input.toDoubleOrNull()
                    if (parsed != null && parsed >= 0.0) {
                        onRateChange(parsed)
                    }
                },
                label = { Text("Rate $/g", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = CadGold, modifier = Modifier.size(16.dp))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CadGold,
                    unfocusedBorderColor = CadBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .width(115.dp)
                    .testTag("rate_input")
            )
        }
    }
}

@Composable
private fun WeightCard(
    label: String,
    value: String,
    subtitle: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlight) CadGold.copy(alpha = 0.12f) else Color(0xFF1E293B))
            .border(1.dp, if (highlight) CadGold.copy(alpha = 0.5f) else CadBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (highlight) CadGold else Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = if (highlight) 16.sp else 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = Color(0xFF94A3B8),
            fontSize = 9.sp
        )
    }
}
