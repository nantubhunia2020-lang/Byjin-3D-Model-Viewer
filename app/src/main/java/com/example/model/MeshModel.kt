package com.example.model

import java.nio.FloatBuffer

data class BoundingBox(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minZ: Float,
    val maxZ: Float
) {
    val sizeX: Float get() = (maxX - minX).coerceAtLeast(0.0001f)
    val sizeY: Float get() = (maxY - minY).coerceAtLeast(0.0001f)
    val sizeZ: Float get() = (maxZ - minZ).coerceAtLeast(0.0001f)
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f
    val centerZ: Float get() = (minZ + maxZ) / 2f
    val maxDimension: Float get() = maxOf(sizeX, maxOf(sizeY, sizeZ))
}

data class MeshModel(
    val name: String,
    val format: String,
    val fileSizeBytes: Long,
    val vertexBuffer: FloatBuffer,
    val normalBuffer: FloatBuffer,
    val wireframeBuffer: FloatBuffer? = null,
    val vertexCount: Int,
    val triangleCount: Int,
    val boundingBox: BoundingBox,
    val surfaceAreaMm2: Double,
    val volumeMm3: Double,
    val isWatertight: Boolean = true
) {
    // Formatted helper properties
    val surfaceAreaCm2: Double get() = surfaceAreaMm2 / 100.0
    val volumeCm3: Double get() = volumeMm3 / 1000.0

    /**
     * Estimates weight in grams based on density in g/cm3 and scale factor
     */
    fun calculateWeightGrams(densityGCm3: Double, scale: Float = 1.0f): Double {
        val scaledVolumeCm3 = volumeCm3 * (scale * scale * scale)
        return (scaledVolumeCm3 * densityGCm3).coerceAtLeast(0.0)
    }

    /**
     * Weight in pennyweights (dwt) - standard in jewelry
     */
    fun calculateWeightDwt(densityGCm3: Double, scale: Float = 1.0f): Double {
        return calculateWeightGrams(densityGCm3, scale) * 0.643014931
    }
}
