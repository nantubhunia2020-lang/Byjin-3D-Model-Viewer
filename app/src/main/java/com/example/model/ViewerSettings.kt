package com.example.model

enum class CrossSectionAxis {
    Z_AXIS,
    Y_AXIS,
    X_AXIS
}

enum class CadViewAngle {
    ISOMETRIC,
    TOP,
    BOTTOM,
    FRONT,
    RIGHT,
    LEFT
}

data class ViewerSettings(
    val renderStyle: RenderStyle = RenderStyle.METALLIC,
    val selectedMaterial: MaterialPreset = MaterialPreset.YELLOW_GOLD_18K,
    val showBoundingBox: Boolean = true,
    val showGrid: Boolean = true,
    val showWireframeOverlay: Boolean = false,
    val isTurntableActive: Boolean = false,
    val turntableSpeed: Float = 0.5f,
    val isCrossSectionEnabled: Boolean = false,
    val crossSectionAxis: CrossSectionAxis = CrossSectionAxis.Z_AXIS,
    val crossSectionProgress: Float = 1.0f, // 0.0 to 1.0
    val scaleFactor: Float = 1.0f, // 0.1 to 5.0
    val customMaterialRateUsd: Double? = null,
    val isMetric: Boolean = true // mm vs inch
)
