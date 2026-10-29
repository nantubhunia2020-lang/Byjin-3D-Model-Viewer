package com.example.parser

import com.example.model.BoundingBox
import com.example.model.MeshModel
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.sqrt

object StlParser {

    /**
     * Parses an STL input stream, auto-detecting Binary vs ASCII format.
     */
    fun parse(inputStream: InputStream, fileName: String, fileSizeBytes: Long): MeshModel {
        val bytes = inputStream.readBytes()
        val size = if (fileSizeBytes > 0) fileSizeBytes else bytes.size.toLong()

        return if (isAsciiStl(bytes)) {
            parseAscii(bytes, fileName, size)
        } else {
            parseBinary(bytes, fileName, size)
        }
    }

    private fun isAsciiStl(bytes: ByteArray): Boolean {
        if (bytes.size < 84) return true
        val headerPrefix = String(bytes, 0, minOf(80, bytes.size), Charsets.US_ASCII).trim()
        if (headerPrefix.startsWith("solid", ignoreCase = true)) {
            // Check if binary header also happens to start with "solid"
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val triangleCount = buffer.getInt(80)
            val expectedBinarySize = 80 + 4 + (triangleCount * 50L)
            if (expectedBinarySize == bytes.size.toLong() && triangleCount > 0) {
                return false // Real binary STL with "solid" header
            }
            return true
        }
        return false
    }

    private fun parseBinary(bytes: ByteArray, fileName: String, fileSizeBytes: Long): MeshModel {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(80)
        val triangleCount = buffer.getInt().coerceAtLeast(0)

        // Safety check
        val actualTriangles = minOf(triangleCount, ((bytes.size - 84) / 50).coerceAtLeast(0))
        val vertexCount = actualTriangles * 3

        val vertexFloatBuffer = ByteBuffer.allocateDirect(vertexCount * 3 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        val normalFloatBuffer = ByteBuffer.allocateDirect(vertexCount * 3 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        // Wireframe buffer: 3 lines per triangle = 6 vertices per triangle
        val wireframeFloatBuffer = ByteBuffer.allocateDirect(actualTriangles * 6 * 3 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        var totalSurfaceArea = 0.0
        var totalSignedVolume = 0.0

        for (i in 0 until actualTriangles) {
            val nx = buffer.getFloat()
            val ny = buffer.getFloat()
            val nz = buffer.getFloat()

            val v1x = buffer.getFloat()
            val v1y = buffer.getFloat()
            val v1z = buffer.getFloat()

            val v2x = buffer.getFloat()
            val v2y = buffer.getFloat()
            val v2z = buffer.getFloat()

            val v3x = buffer.getFloat()
            val v3y = buffer.getFloat()
            val v3z = buffer.getFloat()

            // Skip 2 attribute bytes
            buffer.short

            // Calculate face normal if stored normal is 0
            val norm = if (abs(nx) < 1e-6f && abs(ny) < 1e-6f && abs(nz) < 1e-6f) {
                calculateFaceNormal(v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z)
            } else {
                floatArrayOf(nx, ny, nz)
            }

            // Put vertices
            vertexFloatBuffer.put(v1x).put(v1y).put(v1z)
            vertexFloatBuffer.put(v2x).put(v2y).put(v2z)
            vertexFloatBuffer.put(v3x).put(v3y).put(v3z)

            // Put normals (one per vertex)
            normalFloatBuffer.put(norm[0]).put(norm[1]).put(norm[2])
            normalFloatBuffer.put(norm[0]).put(norm[1]).put(norm[2])
            normalFloatBuffer.put(norm[0]).put(norm[1]).put(norm[2])

            // Put wireframe line segments (v1-v2, v2-v3, v3-v1)
            wireframeFloatBuffer.put(v1x).put(v1y).put(v1z).put(v2x).put(v2y).put(v2z)
            wireframeFloatBuffer.put(v2x).put(v2y).put(v2z).put(v3x).put(v3y).put(v3z)
            wireframeFloatBuffer.put(v3x).put(v3y).put(v3z).put(v1x).put(v1y).put(v1z)

            // Update bounding box
            minX = minOf(minX, v1x, v2x, v3x)
            maxX = maxOf(maxX, v1x, v2x, v3x)
            minY = minOf(minY, v1y, v2y, v3y)
            maxY = maxOf(maxY, v1y, v2y, v3y)
            minZ = minOf(minZ, v1z, v2z, v3z)
            maxZ = maxOf(maxZ, v1z, v2z, v3z)

            // Surface Area: 0.5 * |(v2 - v1) x (v3 - v1)|
            val ax = v2x - v1x
            val ay = v2y - v1y
            val az = v2z - v1z
            val bx = v3x - v1x
            val by = v3y - v1y
            val bz = v3z - v1z
            val cx = ay * bz - az * by
            val cy = az * bx - ax * bz
            val cz = ax * by - ay * bx
            val triArea = 0.5 * sqrt((cx * cx + cy * cy + cz * cz).toDouble())
            totalSurfaceArea += triArea

            // Signed Tetrahedron Volume: (v1 . (v2 x v3)) / 6
            val crossX = v2y * v3z - v2z * v3y
            val crossY = v2z * v3x - v2x * v3z
            val crossZ = v2x * v3y - v2y * v3x
            val tetVol = (v1x * crossX + v1y * crossY + v1z * crossZ) / 6.0
            totalSignedVolume += tetVol
        }

        vertexFloatBuffer.position(0)
        normalFloatBuffer.position(0)
        wireframeFloatBuffer.position(0)

        val bbox = BoundingBox(
            minX = if (actualTriangles > 0) minX else -1f,
            maxX = if (actualTriangles > 0) maxX else 1f,
            minY = if (actualTriangles > 0) minY else -1f,
            maxY = if (actualTriangles > 0) maxY else 1f,
            minZ = if (actualTriangles > 0) minZ else -1f,
            maxZ = if (actualTriangles > 0) maxZ else 1f
        )

        return MeshModel(
            name = fileName,
            format = "STL (Binary)",
            fileSizeBytes = fileSizeBytes,
            vertexBuffer = vertexFloatBuffer,
            normalBuffer = normalFloatBuffer,
            wireframeBuffer = wireframeFloatBuffer,
            vertexCount = vertexCount,
            triangleCount = actualTriangles,
            boundingBox = bbox,
            surfaceAreaMm2 = totalSurfaceArea,
            volumeMm3 = abs(totalSignedVolume),
            isWatertight = totalSignedVolume > 0.0001
        )
    }

    private fun parseAscii(bytes: ByteArray, fileName: String, fileSizeBytes: Long): MeshModel {
        val text = String(bytes, Charsets.UTF_8)
        val lines = text.lineSequence()

        val vertices = ArrayList<Float>()
        val normals = ArrayList<Float>()
        val currentFacetVerts = ArrayList<Float>()

        var currentNx = 0f
        var currentNy = 0f
        var currentNz = 1f

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        var totalSurfaceArea = 0.0
        var totalSignedVolume = 0.0
        var triangleCount = 0

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.startsWith("facet normal", ignoreCase = true)) {
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 4) {
                    currentNx = parts[2].toFloatOrNull() ?: 0f
                    currentNy = parts[3].toFloatOrNull() ?: 0f
                    currentNz = parts[4].toFloatOrNull() ?: 0f
                }
                currentFacetVerts.clear()
            } else if (line.startsWith("vertex", ignoreCase = true)) {
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 4) {
                    val x = parts[1].toFloatOrNull() ?: 0f
                    val y = parts[2].toFloatOrNull() ?: 0f
                    val z = parts[3].toFloatOrNull() ?: 0f
                    currentFacetVerts.add(x)
                    currentFacetVerts.add(y)
                    currentFacetVerts.add(z)
                }
            } else if (line.startsWith("endfacet", ignoreCase = true)) {
                if (currentFacetVerts.size >= 9) {
                    val v1x = currentFacetVerts[0]
                    val v1y = currentFacetVerts[1]
                    val v1z = currentFacetVerts[2]

                    val v2x = currentFacetVerts[3]
                    val v2y = currentFacetVerts[4]
                    val v2z = currentFacetVerts[5]

                    val v3x = currentFacetVerts[6]
                    val v3y = currentFacetVerts[7]
                    val v3z = currentFacetVerts[8]

                    val norm = if (abs(currentNx) < 1e-6f && abs(currentNy) < 1e-6f && abs(currentNz) < 1e-6f) {
                        calculateFaceNormal(v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z)
                    } else {
                        floatArrayOf(currentNx, currentNy, currentNz)
                    }

                    vertices.add(v1x); vertices.add(v1y); vertices.add(v1z)
                    vertices.add(v2x); vertices.add(v2y); vertices.add(v2z)
                    vertices.add(v3x); vertices.add(v3y); vertices.add(v3z)

                    normals.add(norm[0]); normals.add(norm[1]); normals.add(norm[2])
                    normals.add(norm[0]); normals.add(norm[1]); normals.add(norm[2])
                    normals.add(norm[0]); normals.add(norm[1]); normals.add(norm[2])

                    minX = minOf(minX, v1x, v2x, v3x)
                    maxX = maxOf(maxX, v1x, v2x, v3x)
                    minY = minOf(minY, v1y, v2y, v3y)
                    maxY = maxOf(maxY, v1y, v2y, v3y)
                    minZ = minOf(minZ, v1z, v2z, v3z)
                    maxZ = maxOf(maxZ, v1z, v2z, v3z)

                    // Area & Volume
                    val ax = v2x - v1x; val ay = v2y - v1y; val az = v2z - v1z
                    val bx = v3x - v1x; val by = v3y - v1y; val bz = v3z - v1z
                    val cx = ay * bz - az * by; val cy = az * bx - ax * bz; val cz = ax * by - ay * bx
                    totalSurfaceArea += 0.5 * sqrt((cx * cx + cy * cy + cz * cz).toDouble())

                    val crossX = v2y * v3z - v2z * v3y
                    val crossY = v2z * v3x - v2x * v3z
                    val crossZ = v2x * v3y - v2y * v3x
                    totalSignedVolume += (v1x * crossX + v1y * crossY + v1z * crossZ) / 6.0

                    triangleCount++
                }
                currentFacetVerts.clear()
            }
        }

        val vertexFloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        for (v in vertices) vertexFloatBuffer.put(v)
        vertexFloatBuffer.position(0)

        val normalFloatBuffer = ByteBuffer.allocateDirect(normals.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        for (n in normals) normalFloatBuffer.put(n)
        normalFloatBuffer.position(0)

        val wireframeFloatBuffer = ByteBuffer.allocateDirect(triangleCount * 6 * 3 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        for (i in 0 until triangleCount) {
            val idx = i * 9
            val v1x = vertices[idx]; val v1y = vertices[idx + 1]; val v1z = vertices[idx + 2]
            val v2x = vertices[idx + 3]; val v2y = vertices[idx + 4]; val v2z = vertices[idx + 5]
            val v3x = vertices[idx + 6]; val v3y = vertices[idx + 7]; val v3z = vertices[idx + 8]
            wireframeFloatBuffer.put(v1x).put(v1y).put(v1z).put(v2x).put(v2y).put(v2z)
            wireframeFloatBuffer.put(v2x).put(v2y).put(v2z).put(v3x).put(v3y).put(v3z)
            wireframeFloatBuffer.put(v3x).put(v3y).put(v3z).put(v1x).put(v1y).put(v1z)
        }
        wireframeFloatBuffer.position(0)

        val bbox = BoundingBox(
            minX = if (triangleCount > 0) minX else -1f,
            maxX = if (triangleCount > 0) maxX else 1f,
            minY = if (triangleCount > 0) minY else -1f,
            maxY = if (triangleCount > 0) maxY else 1f,
            minZ = if (triangleCount > 0) minZ else -1f,
            maxZ = if (triangleCount > 0) maxZ else 1f
        )

        return MeshModel(
            name = fileName,
            format = "STL (ASCII)",
            fileSizeBytes = fileSizeBytes,
            vertexBuffer = vertexFloatBuffer,
            normalBuffer = normalFloatBuffer,
            wireframeBuffer = wireframeFloatBuffer,
            vertexCount = vertices.size / 3,
            triangleCount = triangleCount,
            boundingBox = bbox,
            surfaceAreaMm2 = totalSurfaceArea,
            volumeMm3 = abs(totalSignedVolume),
            isWatertight = totalSignedVolume > 0.0001
        )
    }

    private fun calculateFaceNormal(
        v1x: Float, v1y: Float, v1z: Float,
        v2x: Float, v2y: Float, v2z: Float,
        v3x: Float, v3y: Float, v3z: Float
    ): FloatArray {
        val ax = v2x - v1x
        val ay = v2y - v1y
        val az = v2z - v1z
        val bx = v3x - v1x
        val by = v3y - v1y
        val bz = v3z - v1z

        var nx = ay * bz - az * by
        var ny = az * bx - ax * bz
        var nz = ax * by - ay * bx

        val len = sqrt((nx * nx + ny * ny + nz * nz).toDouble()).toFloat()
        if (len > 1e-6f) {
            nx /= len
            ny /= len
            nz /= len
        } else {
            nx = 0f; ny = 0f; nz = 1f
        }
        return floatArrayOf(nx, ny, nz)
    }
}
