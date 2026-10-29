package com.example.parser

import com.example.model.BoundingBox
import com.example.model.MeshModel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object SampleModels {

    data class SampleInfo(
        val id: String,
        val title: String,
        val subtitle: String,
        val formatBadge: String,
        val createModel: () -> MeshModel
    )

    val ALL_SAMPLES = listOf(
        SampleInfo(
            id = "solitaire_ring",
            title = "Solitaire Diamond Ring",
            subtitle = "CAD Jewelry - 18mm Shank & 6-Prong Head",
            formatBadge = "STL"
        ) { createSolitaireRing() },
        SampleInfo(
            id = "mechanical_bracket",
            title = "Turbine Flange Bracket",
            subtitle = "Engineering CAD - Bored Mounting Flange",
            formatBadge = "STL"
        ) { createMechanicalBracket() },
        SampleInfo(
            id = "rhino_sculpture",
            title = "Rhino 3DM Parametric Art",
            subtitle = "Rhino CAD - Faceted Polyhedron Sculpture",
            formatBadge = "3DM"
        ) { createRhinoSculpture("Rhino_Parametric_Sculpture.3dm", 345_200L) },
        SampleInfo(
            id = "jewelry_pendant",
            title = "Filigree Clover Pendant",
            subtitle = "Jewelry Design - Bezel Charm with Bail",
            formatBadge = "STL"
        ) { createJewelryPendant() }
    )

    fun createSolitaireRing(): MeshModel {
        val triangles = ArrayList<FloatArray>()

        // 1. Ring Shank (Torus segment forming full ring: outer R = 10.0mm, inner r = 1.3mm)
        val majorRadius = 9.0f // 18mm inner diameter (Size 7-8)
        val minorRadius = 1.2f
        val majorSteps = 36
        val minorSteps = 16

        for (i in 0 until majorSteps) {
            val u0 = (i.toFloat() / majorSteps) * 2f * PI.toFloat()
            val u1 = ((i + 1).toFloat() / majorSteps) * 2f * PI.toFloat()

            for (j in 0 until minorSteps) {
                val v0 = (j.toFloat() / minorSteps) * 2f * PI.toFloat()
                val v1 = ((j + 1).toFloat() / minorSteps) * 2f * PI.toFloat()

                val p00 = torusPoint(majorRadius, minorRadius, u0, v0)
                val p10 = torusPoint(majorRadius, minorRadius, u1, v0)
                val p11 = torusPoint(majorRadius, minorRadius, u1, v1)
                val p01 = torusPoint(majorRadius, minorRadius, u0, v1)

                triangles.add(floatArrayOf(p00[0], p00[1], p00[2], p10[0], p10[1], p10[2], p11[0], p11[1], p11[2]))
                triangles.add(floatArrayOf(p00[0], p00[1], p00[2], p11[0], p11[1], p11[2], p01[0], p01[1], p01[2]))
            }
        }

        // 2. 6-Prong Head at top of ring (Z around majorRadius + minorRadius = ~10.2mm)
        val headCenterZ = majorRadius + minorRadius
        val prongCount = 6
        val prongRadius = 2.4f
        val prongHeight = 3.6f
        val prongThickness = 0.35f

        for (p in 0 until prongCount) {
            val angle = (p.toFloat() / prongCount) * 2f * PI.toFloat()
            val px = cos(angle) * prongRadius
            val py = sin(angle) * prongRadius

            // Simple cylinder/prism for each prong
            for (seg in 0 until 6) {
                val a0 = (seg.toFloat() / 6) * 2f * PI.toFloat()
                val a1 = ((seg + 1).toFloat() / 6) * 2f * PI.toFloat()

                val b0 = floatArrayOf(px + cos(a0) * prongThickness, py + sin(a0) * prongThickness, headCenterZ)
                val b1 = floatArrayOf(px + cos(a1) * prongThickness, py + sin(a1) * prongThickness, headCenterZ)
                val t0 = floatArrayOf(px * 0.85f + cos(a0) * (prongThickness * 0.7f), py * 0.85f + sin(a0) * (prongThickness * 0.7f), headCenterZ + prongHeight)
                val t1 = floatArrayOf(px * 0.85f + cos(a1) * (prongThickness * 0.7f), py * 0.85f + sin(a1) * (prongThickness * 0.7f), headCenterZ + prongHeight)

                triangles.add(floatArrayOf(b0[0], b0[1], b0[2], b1[0], b1[1], b1[2], t1[0], t1[1], t1[2]))
                triangles.add(floatArrayOf(b0[0], b0[1], b0[2], t1[0], t1[1], t1[2], t0[0], t0[1], t0[2]))
            }
        }

        // 3. Faceted Brilliant Diamond inside prongs
        val gemCenterZ = headCenterZ + prongHeight * 0.65f
        val tableZ = gemCenterZ + 1.2f
        val girdleZ = gemCenterZ + 0.1f
        val culetZ = gemCenterZ - 1.8f
        val girdleRadius = 2.2f
        val tableRadius = 1.3f
        val gemFacets = 16

        // Table octagons/facets
        for (f in 0 until gemFacets) {
            val a0 = (f.toFloat() / gemFacets) * 2f * PI.toFloat()
            val a1 = ((f + 1).toFloat() / gemFacets) * 2f * PI.toFloat()

            val tab0 = floatArrayOf(cos(a0) * tableRadius, sin(a0) * tableRadius, tableZ)
            val tab1 = floatArrayOf(cos(a1) * tableRadius, sin(a1) * tableRadius, tableZ)
            val centerTab = floatArrayOf(0f, 0f, tableZ)

            // Table top
            triangles.add(floatArrayOf(centerTab[0], centerTab[1], centerTab[2], tab0[0], tab0[1], tab0[2], tab1[0], tab1[1], tab1[2]))

            val gir0 = floatArrayOf(cos(a0) * girdleRadius, sin(a0) * girdleRadius, girdleZ)
            val gir1 = floatArrayOf(cos(a1) * girdleRadius, sin(a1) * girdleRadius, girdleZ)

            // Crown facets (table to girdle)
            triangles.add(floatArrayOf(tab0[0], tab0[1], tab0[2], gir0[0], gir0[1], gir0[2], gir1[0], gir1[1], gir1[2]))
            triangles.add(floatArrayOf(tab0[0], tab0[1], tab0[2], gir1[0], gir1[1], gir1[2], tab1[0], tab1[1], tab1[2]))

            // Pavilion facets (girdle to culet)
            val culet = floatArrayOf(0f, 0f, culetZ)
            triangles.add(floatArrayOf(gir0[0], gir0[1], gir0[2], culet[0], culet[1], culet[2], gir1[0], gir1[1], gir1[2]))
        }

        return buildMeshFromTriangles(triangles, "Solitaire_Ring_18mm.stl", "STL (Binary)", 184_200L)
    }

    private fun torusPoint(R: Float, r: Float, u: Float, v: Float): FloatArray {
        val x = (R + r * cos(v)) * cos(u)
        val y = (R + r * cos(v)) * sin(u)
        val z = r * sin(v)
        return floatArrayOf(x, y, z)
    }

    fun createMechanicalBracket(): MeshModel {
        val triangles = ArrayList<FloatArray>()

        // Base Flange Plate: 30mm x 20mm x 4mm with center boss
        addBox(triangles, -15f, 15f, -10f, 10f, 0f, 4f)

        // Center Cylindrical Boss: radius 6mm, height 12mm
        val bossRadius = 6.0f
        val bossInnerRadius = 3.2f
        val bossZ0 = 4.0f
        val bossZ1 = 16.0f
        val segments = 24

        for (s in 0 until segments) {
            val a0 = (s.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((s + 1).toFloat() / segments) * 2f * PI.toFloat()

            val o0 = floatArrayOf(cos(a0) * bossRadius, sin(a0) * bossRadius, bossZ0)
            val o1 = floatArrayOf(cos(a1) * bossRadius, sin(a1) * bossRadius, bossZ0)
            val ot0 = floatArrayOf(cos(a0) * bossRadius, sin(a0) * bossRadius, bossZ1)
            val ot1 = floatArrayOf(cos(a1) * bossRadius, sin(a1) * bossRadius, bossZ1)

            // Outer wall
            triangles.add(floatArrayOf(o0[0], o0[1], o0[2], o1[0], o1[1], o1[2], ot1[0], ot1[1], ot1[2]))
            triangles.add(floatArrayOf(o0[0], o0[1], o0[2], ot1[0], ot1[1], ot1[2], ot0[0], ot0[1], ot0[2]))

            val i0 = floatArrayOf(cos(a0) * bossInnerRadius, sin(a0) * bossInnerRadius, bossZ1)
            val i1 = floatArrayOf(cos(a1) * bossInnerRadius, sin(a1) * bossInnerRadius, bossZ1)

            // Top rim ring
            triangles.add(floatArrayOf(ot0[0], ot0[1], ot0[2], ot1[0], ot1[1], ot1[2], i1[0], i1[1], i1[2]))
            triangles.add(floatArrayOf(ot0[0], ot0[1], ot0[2], i1[0], i1[1], i1[2], i0[0], i0[1], i0[2]))
        }

        // 2 Side Gusset Ribs for stiffness
        addBox(triangles, -1.5f, 1.5f, 5.5f, 9.5f, 4f, 12f)
        addBox(triangles, -1.5f, 1.5f, -9.5f, -5.5f, 4f, 12f)

        return buildMeshFromTriangles(triangles, "Turbine_Flange_Bracket.stl", "STL (Binary)", 142_500L)
    }

    fun createRhinoSculpture(name: String, sizeBytes: Long): MeshModel {
        val triangles = ArrayList<FloatArray>()
        // Beautiful Icosahedron / Geodesic Faceted Sculpture
        val phi = (1.0f + sqrt(5.0f)) / 2.0f
        val scale = 8.0f

        val rawVertices = arrayOf(
            floatArrayOf(-1f,  phi, 0f), floatArrayOf( 1f,  phi, 0f),
            floatArrayOf(-1f, -phi, 0f), floatArrayOf( 1f, -phi, 0f),
            floatArrayOf(0f, -1f,  phi), floatArrayOf(0f,  1f,  phi),
            floatArrayOf(0f, -1f, -phi), floatArrayOf(0f,  1f, -phi),
            floatArrayOf( phi, 0f, -1f), floatArrayOf( phi, 0f,  1f),
            floatArrayOf(-phi, 0f, -1f), floatArrayOf(-phi, 0f,  1f)
        )

        val faces = arrayOf(
            intArrayOf(0, 11, 5), intArrayOf(0, 5, 1), intArrayOf(0, 1, 7), intArrayOf(0, 7, 10), intArrayOf(0, 10, 11),
            intArrayOf(1, 5, 9), intArrayOf(5, 11, 4), intArrayOf(11, 10, 2), intArrayOf(10, 7, 6), intArrayOf(7, 1, 8),
            intArrayOf(3, 9, 4), intArrayOf(3, 4, 2), intArrayOf(3, 2, 6), intArrayOf(3, 6, 8), intArrayOf(3, 8, 9),
            intArrayOf(4, 9, 5), intArrayOf(2, 4, 11), intArrayOf(6, 2, 10), intArrayOf(8, 6, 7), intArrayOf(9, 8, 1)
        )

        // Stellated points for faceted parametric sculpture
        for (face in faces) {
            val v0 = rawVertices[face[0]]
            val v1 = rawVertices[face[1]]
            val v2 = rawVertices[face[2]]

            // Calculate face center elevated outward (stellation)
            val cx = (v0[0] + v1[0] + v2[0]) / 3f * 1.55f * scale
            val cy = (v0[1] + v1[1] + v2[1]) / 3f * 1.55f * scale
            val cz = (v0[2] + v1[2] + v2[2]) / 3f * 1.55f * scale

            val p0 = floatArrayOf(v0[0] * scale, v0[1] * scale, v0[2] * scale)
            val p1 = floatArrayOf(v1[0] * scale, v1[1] * scale, v1[2] * scale)
            val p2 = floatArrayOf(v2[0] * scale, v2[1] * scale, v2[2] * scale)

            triangles.add(floatArrayOf(p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], cx, cy, cz))
            triangles.add(floatArrayOf(p1[0], p1[1], p1[2], p2[0], p2[1], p2[2], cx, cy, cz))
            triangles.add(floatArrayOf(p2[0], p2[1], p2[2], p0[0], p0[1], p0[2], cx, cy, cz))
        }

        return buildMeshFromTriangles(triangles, name, "Rhino (3DM OpenNURBS)", sizeBytes)
    }

    fun createJewelryPendant(): MeshModel {
        val triangles = ArrayList<FloatArray>()
        // 4-leaf clover filigree medallion
        val petalCount = 4
        val petalRadius = 4.5f
        val petalDistance = 5.0f
        val thickness = 1.8f

        for (p in 0 until petalCount) {
            val angle = (p.toFloat() / petalCount) * 2f * PI.toFloat()
            val cx = cos(angle) * petalDistance
            val cy = sin(angle) * petalDistance

            // Draw petal cylinder
            val segs = 16
            for (s in 0 until segs) {
                val a0 = (s.toFloat() / segs) * 2f * PI.toFloat()
                val a1 = ((s + 1).toFloat() / segs) * 2f * PI.toFloat()

                val b0 = floatArrayOf(cx + cos(a0) * petalRadius, cy + sin(a0) * petalRadius, 0f)
                val b1 = floatArrayOf(cx + cos(a1) * petalRadius, cy + sin(a1) * petalRadius, 0f)
                val t0 = floatArrayOf(cx + cos(a0) * petalRadius, cy + sin(a0) * petalRadius, thickness)
                val t1 = floatArrayOf(cx + cos(a1) * petalRadius, cy + sin(a1) * petalRadius, thickness)

                // Outer edge
                triangles.add(floatArrayOf(b0[0], b0[1], b0[2], b1[0], b1[1], b1[2], t1[0], t1[1], t1[2]))
                triangles.add(floatArrayOf(b0[0], b0[1], b0[2], t1[0], t1[1], t1[2], t0[0], t0[1], t0[2]))

                // Top & Bottom caps
                val centerTop = floatArrayOf(cx, cy, thickness)
                val centerBot = floatArrayOf(cx, cy, 0f)
                triangles.add(floatArrayOf(centerTop[0], centerTop[1], centerTop[2], t0[0], t0[1], t0[2], t1[0], t1[1], t1[2]))
                triangles.add(floatArrayOf(centerBot[0], centerBot[1], centerBot[2], b1[0], b1[1], b1[2], b0[0], b0[1], b0[2]))
            }
        }

        // Top Bail Loop for chain
        val bailZ = 10.5f
        val bailOuterR = 2.4f
        val bailInnerR = 1.4f
        for (s in 0 until 12) {
            val a0 = (s.toFloat() / 12) * 2f * PI.toFloat()
            val a1 = ((s + 1).toFloat() / 12) * 2f * PI.toFloat()
            val p0 = floatArrayOf(0f, bailZ + cos(a0) * bailOuterR, thickness * 0.5f + sin(a0) * bailOuterR)
            val p1 = floatArrayOf(0f, bailZ + cos(a1) * bailOuterR, thickness * 0.5f + sin(a1) * bailOuterR)
            val pi0 = floatArrayOf(0f, bailZ + cos(a0) * bailInnerR, thickness * 0.5f + sin(a0) * bailInnerR)
            val pi1 = floatArrayOf(0f, bailZ + cos(a1) * bailInnerR, thickness * 0.5f + sin(a1) * bailInnerR)

            triangles.add(floatArrayOf(p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], pi1[0], pi1[1], pi1[2]))
            triangles.add(floatArrayOf(p0[0], p0[1], p0[2], pi1[0], pi1[1], pi1[2], pi0[0], pi0[1], pi0[2]))
        }

        return buildMeshFromTriangles(triangles, "Filigree_Clover_Pendant.stl", "STL (Binary)", 98_000L)
    }

    private fun addBox(triangles: ArrayList<FloatArray>, x0: Float, x1: Float, y0: Float, y1: Float, z0: Float, z1: Float) {
        val p000 = floatArrayOf(x0, y0, z0); val p100 = floatArrayOf(x1, y0, z0)
        val p110 = floatArrayOf(x1, y1, z0); val p010 = floatArrayOf(x0, y1, z0)
        val p001 = floatArrayOf(x0, y0, z1); val p101 = floatArrayOf(x1, y0, z1)
        val p111 = floatArrayOf(x1, y1, z1); val p011 = floatArrayOf(x0, y1, z1)

        // Bottom (-Z)
        triangles.add(floatArrayOf(p000[0], p000[1], p000[2], p110[0], p110[1], p110[2], p100[0], p100[1], p100[2]))
        triangles.add(floatArrayOf(p000[0], p000[1], p000[2], p010[0], p010[1], p010[2], p110[0], p110[1], p110[2]))
        // Top (+Z)
        triangles.add(floatArrayOf(p001[0], p001[1], p001[2], p101[0], p101[1], p101[2], p111[0], p111[1], p111[2]))
        triangles.add(floatArrayOf(p001[0], p001[1], p001[2], p111[0], p111[1], p111[2], p011[0], p011[1], p011[2]))
        // Front (-Y)
        triangles.add(floatArrayOf(p000[0], p000[1], p000[2], p100[0], p100[1], p100[2], p101[0], p101[1], p101[2]))
        triangles.add(floatArrayOf(p000[0], p000[1], p000[2], p101[0], p101[1], p101[2], p001[0], p001[1], p001[2]))
        // Back (+Y)
        triangles.add(floatArrayOf(p010[0], p010[1], p010[2], p111[0], p111[1], p111[2], p110[0], p110[1], p110[2]))
        triangles.add(floatArrayOf(p010[0], p010[1], p010[2], p011[0], p011[1], p011[2], p111[0], p111[1], p111[2]))
        // Left (-X)
        triangles.add(floatArrayOf(p000[0], p000[1], p000[2], p001[0], p001[1], p001[2], p011[0], p011[1], p011[2]))
        triangles.add(floatArrayOf(p000[0], p000[1], p000[2], p011[0], p011[1], p011[2], p010[0], p010[1], p010[2]))
        // Right (+X)
        triangles.add(floatArrayOf(p100[0], p100[1], p100[2], p110[0], p110[1], p110[2], p111[0], p111[1], p111[2]))
        triangles.add(floatArrayOf(p100[0], p100[1], p100[2], p111[0], p111[1], p111[2], p101[0], p101[1], p101[2]))
    }

    private fun buildMeshFromTriangles(
        triangles: List<FloatArray>,
        name: String,
        format: String,
        fileSizeBytes: Long
    ): MeshModel {
        val triangleCount = triangles.size
        val vertexCount = triangleCount * 3

        val vertexBuffer = ByteBuffer.allocateDirect(vertexCount * 3 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        val normalBuffer = ByteBuffer.allocateDirect(vertexCount * 3 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        val wireframeBuffer = ByteBuffer.allocateDirect(triangleCount * 6 * 3 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()

        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        var surfaceArea = 0.0
        var totalSignedVolume = 0.0

        for (tri in triangles) {
            val v1x = tri[0]; val v1y = tri[1]; val v1z = tri[2]
            val v2x = tri[3]; val v2y = tri[4]; val v2z = tri[5]
            val v3x = tri[6]; val v3y = tri[7]; val v3z = tri[8]

            val ax = v2x - v1x; val ay = v2y - v1y; val az = v2z - v1z
            val bx = v3x - v1x; val by = v3y - v1y; val bz = v3z - v1z
            var nx = ay * bz - az * by
            var ny = az * bx - ax * bz
            var nz = ax * by - ay * bx
            val len = sqrt((nx * nx + ny * ny + nz * nz).toDouble()).toFloat()
            if (len > 1e-6f) { nx /= len; ny /= len; nz /= len } else { nx = 0f; ny = 0f; nz = 1f }

            vertexBuffer.put(v1x).put(v1y).put(v1z)
            vertexBuffer.put(v2x).put(v2y).put(v2z)
            vertexBuffer.put(v3x).put(v3y).put(v3z)

            normalBuffer.put(nx).put(ny).put(nz)
            normalBuffer.put(nx).put(ny).put(nz)
            normalBuffer.put(nx).put(ny).put(nz)

            wireframeBuffer.put(v1x).put(v1y).put(v1z).put(v2x).put(v2y).put(v2z)
            wireframeBuffer.put(v2x).put(v2y).put(v2z).put(v3x).put(v3y).put(v3z)
            wireframeBuffer.put(v3x).put(v3y).put(v3z).put(v1x).put(v1y).put(v1z)

            minX = minOf(minX, v1x, v2x, v3x); maxX = maxOf(maxX, v1x, v2x, v3x)
            minY = minOf(minY, v1y, v2y, v3y); maxY = maxOf(maxY, v1y, v2y, v3y)
            minZ = minOf(minZ, v1z, v2z, v3z); maxZ = maxOf(maxZ, v1z, v2z, v3z)

            surfaceArea += 0.5 * len
            val crossX = v2y * v3z - v2z * v3y
            val crossY = v2z * v3x - v2x * v3z
            val crossZ = v2x * v3y - v2y * v3x
            totalSignedVolume += (v1x * crossX + v1y * crossY + v1z * crossZ) / 6.0
        }

        vertexBuffer.position(0)
        normalBuffer.position(0)
        wireframeBuffer.position(0)

        return MeshModel(
            name = name,
            format = format,
            fileSizeBytes = fileSizeBytes,
            vertexBuffer = vertexBuffer,
            normalBuffer = normalBuffer,
            wireframeBuffer = wireframeBuffer,
            vertexCount = vertexCount,
            triangleCount = triangleCount,
            boundingBox = BoundingBox(minX, maxX, minY, maxY, minZ, maxZ),
            surfaceAreaMm2 = surfaceArea,
            volumeMm3 = abs(totalSignedVolume),
            isWatertight = totalSignedVolume > 0.0001
        )
    }
}
