package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.MaterialPreset
import com.example.parser.SampleModels
import com.example.parser.StlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Byjin 3D Viewer", appName)
    }

    @Test
    fun testProceduralSolitaireRingGeneration() {
        val ring = SampleModels.createSolitaireRing()
        assertNotNull(ring)
        assertTrue(ring.vertexCount > 0)
        assertTrue(ring.triangleCount > 0)
        assertTrue(ring.boundingBox.sizeX > 0f)
        assertTrue(ring.boundingBox.sizeY > 0f)
        assertTrue(ring.boundingBox.sizeZ > 0f)
        assertTrue(ring.volumeMm3 > 0.0)
    }

    @Test
    fun testWeightCalculationForGoldAndPlatinum() {
        val ring = SampleModels.createSolitaireRing()
        val goldWeight = ring.calculateWeightGrams(MaterialPreset.YELLOW_GOLD_18K.densityGCm3)
        val platWeight = ring.calculateWeightGrams(MaterialPreset.PLATINUM_950.densityGCm3)
        val silverWeight = ring.calculateWeightGrams(MaterialPreset.STERLING_SILVER.densityGCm3)

        // Platinum is denser than 18K gold, which is denser than silver
        assertTrue(platWeight > goldWeight)
        assertTrue(goldWeight > silverWeight)
        assertTrue(goldWeight > 0.0)
    }

    @Test
    fun testAsciiStlParsing() {
        val asciiStl = """
            solid test_cube
              facet normal 0.0 0.0 1.0
                outer loop
                  vertex 0.0 0.0 0.0
                  vertex 10.0 0.0 0.0
                  vertex 10.0 10.0 0.0
                endloop
              endfacet
              facet normal 0.0 0.0 1.0
                outer loop
                  vertex 0.0 0.0 0.0
                  vertex 10.0 10.0 0.0
                  vertex 0.0 10.0 0.0
                endloop
              endfacet
            endsolid test_cube
        """.trimIndent()

        val model = StlParser.parse(
            ByteArrayInputStream(asciiStl.toByteArray(Charsets.UTF_8)),
            "test_cube.stl",
            asciiStl.length.toLong()
        )

        assertEquals(2, model.triangleCount)
        assertEquals(6, model.vertexCount)
        assertEquals(0f, model.boundingBox.minX, 0.001f)
        assertEquals(10f, model.boundingBox.maxX, 0.001f)
        assertEquals(100.0, model.surfaceAreaMm2, 0.01)
    }
}
