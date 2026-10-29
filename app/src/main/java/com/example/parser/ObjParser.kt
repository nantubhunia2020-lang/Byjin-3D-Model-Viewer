package com.example.parser

import com.example.model.BoundingBox
import com.example.model.MeshModel
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

object ObjParser {

    fun parse(inputStream: InputStream, fileName: String, fileSizeBytes: Long): MeshModel {
        val bytes = inputStream.readBytes()
        val text = String(bytes, Charsets.UTF_8)

        val rawPositions = ArrayList<Float>()
        val rawNormals = ArrayList<Float>()

        val finalVertices = ArrayList<Float>()
        val finalNormals = ArrayList<Float>()

        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        var surfaceArea = 0.0
        var totalSignedVolume = 0.0
        var triangleCount = 0

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val tokens = line.split("\\s+".toRegex())
            when (tokens[0]) {
                "v" -> {
                    if (tokens.size >= 4) {
                        rawPositions.add(tokens[1].toFloatOrNull() ?: 0f)
                        rawPositions.add(tokens[2].toFloatOrNull() ?: 0f)
                        rawPositions.add(tokens[3].toFloatOrNull() ?: 0f)
                    }
                }
                "vn" -> {
                    if (tokens.size >= 4) {
                        rawNormals.add(tokens[1].toFloatOrNull() ?: 0f)
                        rawNormals.add(tokens[2].toFloatOrNull() ?: 0f)
                        rawNormals.add(tokens[3].toFloatOrNull() ?: 0f)
                    }
                }
                "f" -> {
                    if (tokens.size >= 4) {
                        val faceIndices = ArrayList<IntArray>() // [vIdx, vtIdx, vnIdx]
                        for (i in 1 until tokens.size) {
                            val parts = tokens[i].split("/")
                            val vIdx = (parts.getOrNull(0)?.toIntOrNull() ?: 1)
                            val vnIdx = (parts.getOrNull(2)?.toIntOrNull() ?: 0)
                            faceIndices.add(intArrayOf(vIdx, vnIdx))
                        }

                        // Triangulate face (fan triangulation for polygons/quads)
                        for (i in 1 until faceIndices.size - 1) {
                            val idx0 = faceIndices[0]
                            val idx1 = faceIndices[i]
                            val idx2 = faceIndices[i + 1]

                            val v1 = getVertex(rawPositions, idx0[0])
                            val v2 = getVertex(rawPositions, idx1[0])
                            val v3 = getVertex(rawPositions, idx2[0])

                            val n1 = getNormal(rawNormals, idx0[1], v1, v2, v3)
                            val n2 = getNormal(rawNormals, idx1[1], v1, v2, v3)
                            val n3 = getNormal(rawNormals, idx2[1], v1, v2, v3)

                            finalVertices.add(v1[0]); finalVertices.add(v1[1]); finalVertices.add(v1[2])
                            finalVertices.add(v2[0]); finalVertices.add(v2[1]); finalVertices.add(v2[2])
                            finalVertices.add(v3[0]); finalVertices.add(v3[1]); finalVertices.add(v3[2])

                            finalNormals.add(n1[0]); finalNormals.add(n1[1]); finalNormals.add(n1[2])
                            finalNormals.add(n2[0]); finalNormals.add(n2[1]); finalNormals.add(n2[2])
                            finalNormals.add(n3[0]); finalNormals.add(n3[1]); finalNormals.add(n3[2])

                            minX = minOf(minX, v1[0], v2[0], v3[0])
                            maxX = maxOf(maxX, v1[0], v2[0], v3[0])
                            minY = minOf(minY, v1[1], v2[1], v3[1])
                            maxY = maxOf(maxY, v1[1], v2[1], v3[1])
                            minZ = minOf(minZ, v1[2], v2[2], v3[2])
                            maxZ = maxOf(maxZ, v1[2], v2[2], v3[2])

                            val ax = v2[0] - v1[0]; val ay = v2[1] - v1[1]; val az = v2[2] - v1[2]
                            val bx = v3[0] - v1[0]; val by = v3[1] - v1[1]; val bz = v3[2] - v1[2]
                            val cx = ay * bz - az * by; val cy = az * bx - ax * bz; val cz = ax * by - ay * bx
                            surfaceArea += 0.5 * sqrt((cx * cx + cy * cy + cz * cz).toDouble())

                            val crossX = v2[1] * v3[2] - v2[2] * v3[1]
                            val crossY = v2[2] * v3[0] - v2[0] * v3[2]
                            val crossZ = v2[0] * v3[1] - v2[1] * v3[0]
                            totalSignedVolume += (v1[0] * crossX + v1[1] * crossY + v1[2] * crossZ) / 6.0

                            triangleCount++
                        }
                    }
                }
            }
        }

        val vertexBuffer = ByteBuffer.allocateDirect(finalVertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (v in finalVertices) vertexBuffer.put(v)
        vertexBuffer.position(0)

        val normalBuffer = ByteBuffer.allocateDirect(finalNormals.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (n in finalNormals) normalBuffer.put(n)
        normalBuffer.position(0)

        val wireframeBuffer = ByteBuffer.allocateDirect(triangleCount * 6 * 3 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (i in 0 until triangleCount) {
            val idx = i * 9
            val v1x = finalVertices[idx]; val v1y = finalVertices[idx + 1]; val v1z = finalVertices[idx + 2]
            val v2x = finalVertices[idx + 3]; val v2y = finalVertices[idx + 4]; val v2z = finalVertices[idx + 5]
            val v3x = finalVertices[idx + 6]; val v3y = finalVertices[idx + 7]; val v3z = finalVertices[idx + 8]
            wireframeBuffer.put(v1x).put(v1y).put(v1z).put(v2x).put(v2y).put(v2z)
            wireframeBuffer.put(v2x).put(v2y).put(v2z).put(v3x).put(v3y).put(v3z)
            wireframeBuffer.put(v3x).put(v3y).put(v3z).put(v1x).put(v1y).put(v1z)
        }
        wireframeBuffer.position(0)

        return MeshModel(
            name = fileName,
            format = "Wavefront (OBJ)",
            fileSizeBytes = fileSizeBytes,
            vertexBuffer = vertexBuffer,
            normalBuffer = normalBuffer,
            wireframeBuffer = wireframeBuffer,
            vertexCount = finalVertices.size / 3,
            triangleCount = triangleCount,
            boundingBox = BoundingBox(minX, maxX, minY, maxY, minZ, maxZ),
            surfaceAreaMm2 = surfaceArea,
            volumeMm3 = abs(totalSignedVolume),
            isWatertight = totalSignedVolume > 0.0001
        )
    }

    private fun getVertex(rawPositions: List<Float>, index: Int): FloatArray {
        val i = if (index > 0) index - 1 else (rawPositions.size / 3) + index
        val base = i * 3
        if (base + 2 < rawPositions.size && base >= 0) {
            return floatArrayOf(rawPositions[base], rawPositions[base + 1], rawPositions[base + 2])
        }
        return floatArrayOf(0f, 0f, 0f)
    }

    private fun getNormal(
        rawNormals: List<Float>,
        index: Int,
        v1: FloatArray,
        v2: FloatArray,
        v3: FloatArray
    ): FloatArray {
        if (index > 0) {
            val base = (index - 1) * 3
            if (base + 2 < rawNormals.size) {
                return floatArrayOf(rawNormals[base], rawNormals[base + 1], rawNormals[base + 2])
            }
        }
        // Fallback: calculate face normal
        val ax = v2[0] - v1[0]; val ay = v2[1] - v1[1]; val az = v2[2] - v1[2]
        val bx = v3[0] - v1[0]; val by = v3[1] - v1[1]; val bz = v3[2] - v1[2]
        var nx = ay * bz - az * by; var ny = az * bx - ax * bz; var nz = ax * by - ay * bx
        val len = sqrt((nx * nx + ny * ny + nz * nz).toDouble()).toFloat()
        if (len > 1e-6f) { nx /= len; ny /= len; nz /= len } else { nx = 0f; ny = 0f; nz = 1f }
        return floatArrayOf(nx, ny, nz)
    }
}
