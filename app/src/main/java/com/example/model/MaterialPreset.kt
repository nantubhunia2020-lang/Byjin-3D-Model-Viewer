package com.example.model

enum class RenderStyle {
    SHADED,
    METALLIC,
    WIREFRAME,
    TRANSPARENT,
    NORMALS
}

data class MaterialPreset(
    val id: String,
    val name: String,
    val category: String,
    val densityGCm3: Double, // Density in grams per cubic centimeter
    val defaultPricePerGramUsd: Double,
    val ambientColor: FloatArray,
    val diffuseColor: FloatArray,
    val specularColor: FloatArray,
    val shininess: Float,
    val alpha: Float = 1.0f,
    val isMetallic: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MaterialPreset
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        val YELLOW_GOLD_18K = MaterialPreset(
            id = "gold_18k",
            name = "18K Yellow Gold",
            category = "Jewelry Metal",
            densityGCm3 = 15.58,
            defaultPricePerGramUsd = 68.50,
            ambientColor = floatArrayOf(0.24725f, 0.1995f, 0.0745f, 1.0f),
            diffuseColor = floatArrayOf(0.75164f, 0.60648f, 0.22648f, 1.0f),
            specularColor = floatArrayOf(0.828281f, 0.755802f, 0.466065f, 1.0f),
            shininess = 64.0f,
            isMetallic = true
        )

        val YELLOW_GOLD_24K = MaterialPreset(
            id = "gold_24k",
            name = "24K Pure Gold",
            category = "Jewelry Metal",
            densityGCm3 = 19.32,
            defaultPricePerGramUsd = 88.00,
            ambientColor = floatArrayOf(0.25f, 0.20f, 0.05f, 1.0f),
            diffuseColor = floatArrayOf(0.85f, 0.70f, 0.15f, 1.0f),
            specularColor = floatArrayOf(0.95f, 0.85f, 0.40f, 1.0f),
            shininess = 80.0f,
            isMetallic = true
        )

        val ROSE_GOLD_14K = MaterialPreset(
            id = "rose_gold_14k",
            name = "14K Rose Gold",
            category = "Jewelry Metal",
            densityGCm3 = 13.07,
            defaultPricePerGramUsd = 52.00,
            ambientColor = floatArrayOf(0.25f, 0.15f, 0.14f, 1.0f),
            diffuseColor = floatArrayOf(0.85f, 0.55f, 0.50f, 1.0f),
            specularColor = floatArrayOf(0.95f, 0.75f, 0.70f, 1.0f),
            shininess = 60.0f,
            isMetallic = true
        )

        val STERLING_SILVER = MaterialPreset(
            id = "silver_925",
            name = "925 Sterling Silver",
            category = "Jewelry Metal",
            densityGCm3 = 10.36,
            defaultPricePerGramUsd = 1.15,
            ambientColor = floatArrayOf(0.19225f, 0.19225f, 0.19225f, 1.0f),
            diffuseColor = floatArrayOf(0.70754f, 0.70754f, 0.70754f, 1.0f),
            specularColor = floatArrayOf(0.808273f, 0.808273f, 0.808273f, 1.0f),
            shininess = 75.0f,
            isMetallic = true
        )

        val PLATINUM_950 = MaterialPreset(
            id = "platinum_950",
            name = "950 Platinum",
            category = "Jewelry Metal",
            densityGCm3 = 21.45,
            defaultPricePerGramUsd = 36.00,
            ambientColor = floatArrayOf(0.23125f, 0.23125f, 0.23125f, 1.0f),
            diffuseColor = floatArrayOf(0.87754f, 0.87754f, 0.87754f, 1.0f),
            specularColor = floatArrayOf(0.933273f, 0.933273f, 0.933273f, 1.0f),
            shininess = 90.0f,
            isMetallic = true
        )

        val BRASS_BRONZE = MaterialPreset(
            id = "brass_bronze",
            name = "Polished Brass",
            category = "Industrial Metal",
            densityGCm3 = 8.50,
            defaultPricePerGramUsd = 0.08,
            ambientColor = floatArrayOf(0.2125f, 0.1275f, 0.054f, 1.0f),
            diffuseColor = floatArrayOf(0.714f, 0.4284f, 0.18144f, 1.0f),
            specularColor = floatArrayOf(0.693548f, 0.50197f, 0.295802f, 1.0f),
            shininess = 45.0f,
            isMetallic = true
        )

        val CASTABLE_RESIN = MaterialPreset(
            id = "castable_resin",
            name = "3D Castable Resin",
            category = "3D Printing",
            densityGCm3 = 1.10,
            defaultPricePerGramUsd = 0.20,
            ambientColor = floatArrayOf(0.1f, 0.18f, 0.22f, 1.0f),
            diffuseColor = floatArrayOf(0.2f, 0.65f, 0.8f, 1.0f),
            specularColor = floatArrayOf(0.4f, 0.7f, 0.85f, 1.0f),
            shininess = 32.0f,
            isMetallic = false
        )

        val PLA_PLASTIC = MaterialPreset(
            id = "pla_plastic",
            name = "PLA Filament (FDM)",
            category = "3D Printing",
            densityGCm3 = 1.24,
            defaultPricePerGramUsd = 0.03,
            ambientColor = floatArrayOf(0.15f, 0.15f, 0.15f, 1.0f),
            diffuseColor = floatArrayOf(0.85f, 0.35f, 0.25f, 1.0f),
            specularColor = floatArrayOf(0.3f, 0.3f, 0.3f, 1.0f),
            shininess = 16.0f,
            isMetallic = false
        )

        val TITANIUM = MaterialPreset(
            id = "titanium",
            name = "Titanium Grade 5",
            category = "Industrial Metal",
            densityGCm3 = 4.51,
            defaultPricePerGramUsd = 0.15,
            ambientColor = floatArrayOf(0.18f, 0.20f, 0.22f, 1.0f),
            diffuseColor = floatArrayOf(0.55f, 0.58f, 0.62f, 1.0f),
            specularColor = floatArrayOf(0.7f, 0.75f, 0.8f, 1.0f),
            shininess = 50.0f,
            isMetallic = true
        )

        val MATTE_CLAY = MaterialPreset(
            id = "matte_clay",
            name = "CAD Studio Gray",
            category = "CAD Preview",
            densityGCm3 = 1.0,
            defaultPricePerGramUsd = 0.0,
            ambientColor = floatArrayOf(0.2f, 0.22f, 0.25f, 1.0f),
            diffuseColor = floatArrayOf(0.6f, 0.65f, 0.7f, 1.0f),
            specularColor = floatArrayOf(0.25f, 0.25f, 0.25f, 1.0f),
            shininess = 12.0f,
            isMetallic = false
        )

        val ALL_PRESETS = listOf(
            YELLOW_GOLD_18K,
            YELLOW_GOLD_24K,
            ROSE_GOLD_14K,
            STERLING_SILVER,
            PLATINUM_950,
            BRASS_BRONZE,
            CASTABLE_RESIN,
            PLA_PLASTIC,
            TITANIUM,
            MATTE_CLAY
        )
    }
}
