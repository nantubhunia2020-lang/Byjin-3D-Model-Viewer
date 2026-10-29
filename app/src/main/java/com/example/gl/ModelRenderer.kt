package com.example.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.model.CadViewAngle
import com.example.model.CrossSectionAxis
import com.example.model.MaterialPreset
import com.example.model.MeshModel
import com.example.model.RenderStyle
import com.example.model.ViewerSettings
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

class ModelRenderer : GLSurfaceView.Renderer {

    @Volatile
    var currentModel: MeshModel? = null
        set(value) {
            field = value
            if (value != null) {
                resetCameraToModel(value)
                updateBoundingBoxBuffers(value)
            }
        }

    @Volatile
    var settings: ViewerSettings = ViewerSettings()

    // Camera & Transform state
    @Volatile var rotationX = 25.0f
    @Volatile var rotationY = -35.0f
    @Volatile var panX = 0.0f
    @Volatile var panY = 0.0f
    @Volatile var cameraDistance = 45.0f
    @Volatile var modelScale = 1.0f

    private var targetCameraDistance = 45.0f
    private var viewportWidth = 1080
    private var viewportHeight = 1920

    // Matrix buffers
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val normalMatrix = FloatArray(9)

    // Shaders
    private var mainProgram = 0
    private var lineProgram = 0

    // Main Shader Handles
    private var uMVPMatrixHandle = 0
    private var uModelMatrixHandle = 0
    private var uNormalMatrixHandle = 0
    private var uLightPosHandle = 0
    private var uCameraPosHandle = 0
    private var uRenderModeHandle = 0
    private var uClipPlaneHandle = 0
    private var uClipEnabledHandle = 0
    private var uAmbientColorHandle = 0
    private var uDiffuseColorHandle = 0
    private var uSpecularColorHandle = 0
    private var uShininessHandle = 0
    private var uAlphaHandle = 0
    private var aPositionHandle = 0
    private var aNormalHandle = 0

    // Line Shader Handles
    private var uLineMVPMatrixHandle = 0
    private var uLineColorHandle = 0
    private var aLinePositionHandle = 0

    // Grid buffers
    private var gridVertexBuffer: FloatBuffer? = null
    private var gridVertexCount = 0

    // Bounding Box buffers
    private var bboxVertexBuffer: FloatBuffer? = null

    // Snapshot callback
    private var snapshotCallback: ((Bitmap) -> Unit)? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.043f, 0.059f, 0.098f, 1.0f) // #0B0F19 Dark CAD
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDisable(GLES20.GL_CULL_FACE) // Double sided for CAD inspection
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        initShaders()
        buildGridBuffers(size = 60f, step = 5f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)

        val aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(projectionMatrix, 0, 45.0f, aspect, 0.1f, 3000.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Turntable spin if enabled
        if (settings.isTurntableActive) {
            rotationY += settings.turntableSpeed
            if (rotationY > 360f) rotationY -= 360f
        }

        // Camera View Matrix
        val eyeX = 0f
        val eyeY = 0f
        val eyeZ = cameraDistance
        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f)

        // Model Matrix: Pan + Orbit + Scale + Centering
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, panX, panY, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f)

        val effectiveScale = modelScale * settings.scaleFactor
        Matrix.scaleM(modelMatrix, 0, effectiveScale, effectiveScale, effectiveScale)

        // Center model at origin
        val model = currentModel
        if (model != null) {
            val bbox = model.boundingBox
            Matrix.translateM(modelMatrix, 0, -bbox.centerX, -bbox.centerY, -bbox.centerZ)
        }

        // Compute MVP matrix
        val vpMatrix = FloatArray(16)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        // Compute 3x3 normal matrix (transpose of inverse of model matrix)
        computeNormalMatrix(modelMatrix, normalMatrix)

        // 1. Draw CAD Ground Grid
        if (settings.showGrid) {
            drawGrid(vpMatrix)
        }

        // 2. Draw 3D Model
        if (model != null && mainProgram != 0) {
            drawModel(model, mvpMatrix, modelMatrix, normalMatrix, eyeX, eyeY, eyeZ)
        }

        // 3. Draw Bounding Box Cage
        if (settings.showBoundingBox && bboxVertexBuffer != null && lineProgram != 0) {
            drawBoundingBox(mvpMatrix)
        }

        // Handle snapshot if requested
        snapshotCallback?.let { callback ->
            val bitmap = captureBitmap(viewportWidth, viewportHeight)
            callback(bitmap)
            snapshotCallback = null
        }
    }

    private fun drawModel(
        model: MeshModel,
        mvp: FloatArray,
        modelMat: FloatArray,
        normalMat: FloatArray,
        eyeX: Float, eyeY: Float, eyeZ: Float
    ) {
        GLES20.glUseProgram(mainProgram)

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMat, 0)
        GLES20.glUniformMatrix3fv(uNormalMatrixHandle, 1, false, normalMat, 0)

        // Dynamic light position
        GLES20.glUniform3f(uLightPosHandle, 50f, 80f, 100f)
        GLES20.glUniform3f(uCameraPosHandle, eyeX, eyeY, eyeZ)

        // Material & Mode
        val material = settings.selectedMaterial
        val renderModeInt = when (settings.renderStyle) {
            RenderStyle.SHADED -> 0
            RenderStyle.METALLIC -> 0
            RenderStyle.NORMALS -> 1
            RenderStyle.TRANSPARENT -> 0
            RenderStyle.WIREFRAME -> 2
        }
        GLES20.glUniform1i(uRenderModeHandle, renderModeInt)

        val alpha = if (settings.renderStyle == RenderStyle.TRANSPARENT) 0.45f else material.alpha
        GLES20.glUniform1f(uAlphaHandle, alpha)

        GLES20.glUniform4fv(uAmbientColorHandle, 1, material.ambientColor, 0)
        GLES20.glUniform4fv(uDiffuseColorHandle, 1, material.diffuseColor, 0)
        GLES20.glUniform4fv(uSpecularColorHandle, 1, material.specularColor, 0)
        GLES20.glUniform1f(uShininessHandle, material.shininess)

        // Cross Section / Clipping Plane
        if (settings.isCrossSectionEnabled) {
            GLES20.glUniform1i(uClipEnabledHandle, 1)
            val bbox = model.boundingBox
            val t = settings.crossSectionProgress // 0..1
            val clipPlane = when (settings.crossSectionAxis) {
                CrossSectionAxis.Z_AXIS -> {
                    val zVal = bbox.minZ + t * bbox.sizeZ
                    floatArrayOf(0f, 0f, -1f, zVal)
                }
                CrossSectionAxis.Y_AXIS -> {
                    val yVal = bbox.minY + t * bbox.sizeY
                    floatArrayOf(0f, -1f, 0f, yVal)
                }
                CrossSectionAxis.X_AXIS -> {
                    val xVal = bbox.minX + t * bbox.sizeX
                    floatArrayOf(-1f, 0f, 0f, xVal)
                }
            }
            GLES20.glUniform4fv(uClipPlaneHandle, 1, clipPlane, 0)
        } else {
            GLES20.glUniform1i(uClipEnabledHandle, 0)
            GLES20.glUniform4f(uClipPlaneHandle, 0f, 0f, 0f, 0f)
        }

        // Vertex positions
        model.vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, model.vertexBuffer)

        // Normals
        model.normalBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aNormalHandle)
        GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, 0, model.normalBuffer)

        // Draw triangles
        if (settings.renderStyle == RenderStyle.WIREFRAME && model.wireframeBuffer != null) {
            // Wireframe only
            drawWireframeLines(model, mvp)
        } else {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, model.vertexCount)

            // Draw wireframe overlay if enabled
            if (settings.showWireframeOverlay && model.wireframeBuffer != null) {
                drawWireframeLines(model, mvp)
            }
        }

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aNormalHandle)
    }

    private fun drawWireframeLines(model: MeshModel, mvp: FloatArray) {
        val buffer = model.wireframeBuffer ?: return
        GLES20.glUseProgram(lineProgram)
        GLES20.glUniformMatrix4fv(uLineMVPMatrixHandle, 1, false, mvp, 0)
        GLES20.glUniform4f(uLineColorHandle, 0.22f, 0.74f, 0.97f, 0.85f) // Cyan wireframe

        buffer.position(0)
        GLES20.glEnableVertexAttribArray(aLinePositionHandle)
        GLES20.glVertexAttribPointer(aLinePositionHandle, 3, GLES20.GL_FLOAT, false, 0, buffer)

        val lineCount = model.triangleCount * 6
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, lineCount)

        GLES20.glDisableVertexAttribArray(aLinePositionHandle)
    }

    private fun drawGrid(vpMatrix: FloatArray) {
        val buf = gridVertexBuffer ?: return
        GLES20.glUseProgram(lineProgram)

        // Grid model matrix located at bottom of model or Y=0
        val gridModelMatrix = FloatArray(16)
        Matrix.setIdentityM(gridModelMatrix, 0)
        val gridMvp = FloatArray(16)
        Matrix.multiplyMM(gridMvp, 0, vpMatrix, 0, gridModelMatrix, 0)

        GLES20.glUniformMatrix4fv(uLineMVPMatrixHandle, 1, false, gridMvp, 0)
        GLES20.glUniform4f(uLineColorHandle, 0.15f, 0.25f, 0.38f, 0.40f) // Subtle grid

        buf.position(0)
        GLES20.glEnableVertexAttribArray(aLinePositionHandle)
        GLES20.glVertexAttribPointer(aLinePositionHandle, 3, GLES20.GL_FLOAT, false, 0, buf)

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, gridVertexCount)
        GLES20.glDisableVertexAttribArray(aLinePositionHandle)
    }

    private fun drawBoundingBox(mvp: FloatArray) {
        val buf = bboxVertexBuffer ?: return
        GLES20.glUseProgram(lineProgram)
        GLES20.glUniformMatrix4fv(uLineMVPMatrixHandle, 1, false, mvp, 0)
        GLES20.glUniform4f(uLineColorHandle, 0.96f, 0.62f, 0.04f, 0.70f) // Gold bounds

        buf.position(0)
        GLES20.glEnableVertexAttribArray(aLinePositionHandle)
        GLES20.glVertexAttribPointer(aLinePositionHandle, 3, GLES20.GL_FLOAT, false, 0, buf)

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 24)
        GLES20.glDisableVertexAttribArray(aLinePositionHandle)
    }

    fun resetCameraToModel(model: MeshModel) {
        val bbox = model.boundingBox
        val maxDim = max(1f, bbox.maxDimension)
        targetCameraDistance = maxDim * 2.2f
        cameraDistance = targetCameraDistance
        panX = 0f
        panY = 0f
        rotationX = 25f
        rotationY = -35f
    }

    fun snapView(angle: CadViewAngle) {
        when (angle) {
            CadViewAngle.ISOMETRIC -> { rotationX = 28f; rotationY = -45f }
            CadViewAngle.TOP -> { rotationX = 90f; rotationY = 0f }
            CadViewAngle.BOTTOM -> { rotationX = -90f; rotationY = 0f }
            CadViewAngle.FRONT -> { rotationX = 0f; rotationY = 0f }
            CadViewAngle.RIGHT -> { rotationX = 0f; rotationY = -90f }
            CadViewAngle.LEFT -> { rotationX = 0f; rotationY = 90f }
        }
        panX = 0f
        panY = 0f
    }

    fun zoom(scaleDelta: Float) {
        cameraDistance = (cameraDistance / scaleDelta).coerceIn(1.0f, 2000.0f)
    }

    fun pan(dx: Float, dy: Float) {
        val factor = cameraDistance * 0.0015f
        panX += dx * factor
        panY -= dy * factor
    }

    fun orbit(dx: Float, dy: Float) {
        rotationY += dx * 0.35f
        rotationX += dy * 0.35f
        rotationX = rotationX.coerceIn(-89.9f, 89.9f)
    }

    fun takeSnapshot(callback: (Bitmap) -> Unit) {
        snapshotCallback = callback
    }

    private fun captureBitmap(width: Int, height: Int): Bitmap {
        val ib = IntBuffer.allocate(width * height)
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib)

        val src = ib.array()
        val dest = IntArray(width * height)

        // Convert RGBA to ARGB and flip vertically
        for (i in 0 until height) {
            for (j in 0 until width) {
                val pix = src[i * width + j]
                val pb = (pix shr 16) and 0xff
                val pr = (pix shl 16) and 0x00ff0000
                val pix1 = (pix and -0xff0100) or pr or pb
                dest[(height - i - 1) * width + j] = pix1
            }
        }
        return Bitmap.createBitmap(dest, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun buildGridBuffers(size: Float, step: Float) {
        val lines = ArrayList<Float>()
        var coord = -size
        while (coord <= size) {
            // Lines parallel to X
            lines.add(-size); lines.add(0f); lines.add(coord)
            lines.add(size); lines.add(0f); lines.add(coord)
            // Lines parallel to Z
            lines.add(coord); lines.add(0f); lines.add(-size)
            lines.add(coord); lines.add(0f); lines.add(size)
            coord += step
        }

        gridVertexCount = lines.size / 3
        gridVertexBuffer = ByteBuffer.allocateDirect(lines.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (v in lines) gridVertexBuffer!!.put(v)
        gridVertexBuffer!!.position(0)
    }

    private fun updateBoundingBoxBuffers(model: MeshModel) {
        val b = model.boundingBox
        val verts = floatArrayOf(
            // Bottom 4 lines
            b.minX, b.minY, b.minZ,   b.maxX, b.minY, b.minZ,
            b.maxX, b.minY, b.minZ,   b.maxX, b.maxY, b.minZ,
            b.maxX, b.maxY, b.minZ,   b.minX, b.maxY, b.minZ,
            b.minX, b.maxY, b.minZ,   b.minX, b.minY, b.minZ,
            // Top 4 lines
            b.minX, b.minY, b.maxZ,   b.maxX, b.minY, b.maxZ,
            b.maxX, b.minY, b.maxZ,   b.maxX, b.maxY, b.maxZ,
            b.maxX, b.maxY, b.maxZ,   b.minX, b.maxY, b.maxZ,
            b.minX, b.maxY, b.maxZ,   b.minX, b.minY, b.maxZ,
            // Vertical 4 pillars
            b.minX, b.minY, b.minZ,   b.minX, b.minY, b.maxZ,
            b.maxX, b.minY, b.minZ,   b.maxX, b.minY, b.maxZ,
            b.maxX, b.maxY, b.minZ,   b.maxX, b.maxY, b.maxZ,
            b.minX, b.maxY, b.minZ,   b.minX, b.maxY, b.maxZ
        )

        bboxVertexBuffer = ByteBuffer.allocateDirect(verts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        bboxVertexBuffer!!.put(verts)
        bboxVertexBuffer!!.position(0)
    }

    private fun computeNormalMatrix(m: FloatArray, n: FloatArray) {
        n[0] = m[0]; n[1] = m[1]; n[2] = m[2]
        n[3] = m[4]; n[4] = m[5]; n[5] = m[6]
        n[6] = m[8]; n[7] = m[9]; n[8] = m[10]
    }

    private fun initShaders() {
        val vertexShaderCode = """
            uniform mat4 u_MVPMatrix;
            uniform mat4 u_ModelMatrix;
            uniform mat3 u_NormalMatrix;
            uniform vec4 u_ClipPlane;
            uniform int u_ClipEnabled;

            attribute vec4 a_Position;
            attribute vec3 a_Normal;

            varying vec3 v_Position;
            varying vec3 v_Normal;
            varying float v_ClipDist;

            void main() {
                vec4 worldPos = u_ModelMatrix * a_Position;
                v_Position = worldPos.xyz;
                v_Normal = normalize(u_NormalMatrix * a_Normal);

                if (u_ClipEnabled == 1) {
                    // dot(clipPlane.xyz, position.xyz) + clipPlane.w
                    v_ClipDist = dot(u_ClipPlane.xyz, a_Position.xyz) + u_ClipPlane.w;
                } else {
                    v_ClipDist = 1.0;
                }

                gl_Position = u_MVPMatrix * a_Position;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;

            uniform vec3 u_LightPos;
            uniform vec3 u_CameraPos;
            uniform int u_RenderMode; // 0 = Shaded/Metallic, 1 = Normals, 2 = Wireframe
            uniform vec4 u_AmbientColor;
            uniform vec4 u_DiffuseColor;
            uniform vec4 u_SpecularColor;
            uniform float u_Shininess;
            uniform float u_Alpha;
            uniform int u_ClipEnabled;

            varying vec3 v_Position;
            varying vec3 v_Normal;
            varying float v_ClipDist;

            void main() {
                if (u_ClipEnabled == 1 && v_ClipDist < 0.0) {
                    discard;
                }

                vec3 norm = normalize(v_Normal);

                if (u_RenderMode == 1) {
                    // Normal visualization
                    gl_FragColor = vec4(abs(norm), 1.0);
                    return;
                }

                if (u_RenderMode == 2) {
                    // Flat Wireframe
                    gl_FragColor = vec4(u_DiffuseColor.rgb, u_Alpha);
                    return;
                }

                // Phong Lighting
                vec3 lightDir = normalize(u_LightPos - v_Position);
                vec3 viewDir = normalize(u_CameraPos - v_Position);
                vec3 halfDir = normalize(lightDir + viewDir);

                // Two-sided normal for cross-sections
                if (!gl_FrontFacing) {
                    norm = -norm;
                }

                // Ambient
                vec3 ambient = u_AmbientColor.rgb;

                // Diffuse
                float diff = max(dot(norm, lightDir), 0.0);
                // Extra hemisphere fill light from below
                float fillDiff = max(dot(norm, vec3(0.0, -1.0, 0.0)), 0.0) * 0.25;
                vec3 diffuse = (diff + fillDiff) * u_DiffuseColor.rgb;

                // Specular (Blinn-Phong)
                float spec = 0.0;
                if (diff > 0.0) {
                    spec = pow(max(dot(norm, halfDir), 0.0), u_Shininess);
                }
                vec3 specular = spec * u_SpecularColor.rgb;

                vec3 finalColor = ambient + diffuse + specular;
                gl_FragColor = vec4(finalColor, u_Alpha);
            }
        """.trimIndent()

        mainProgram = createProgram(vertexShaderCode, fragmentShaderCode)
        uMVPMatrixHandle = GLES20.glGetUniformLocation(mainProgram, "u_MVPMatrix")
        uModelMatrixHandle = GLES20.glGetUniformLocation(mainProgram, "u_ModelMatrix")
        uNormalMatrixHandle = GLES20.glGetUniformLocation(mainProgram, "u_NormalMatrix")
        uLightPosHandle = GLES20.glGetUniformLocation(mainProgram, "u_LightPos")
        uCameraPosHandle = GLES20.glGetUniformLocation(mainProgram, "u_CameraPos")
        uRenderModeHandle = GLES20.glGetUniformLocation(mainProgram, "u_RenderMode")
        uClipPlaneHandle = GLES20.glGetUniformLocation(mainProgram, "u_ClipPlane")
        uClipEnabledHandle = GLES20.glGetUniformLocation(mainProgram, "u_ClipEnabled")
        uAmbientColorHandle = GLES20.glGetUniformLocation(mainProgram, "u_AmbientColor")
        uDiffuseColorHandle = GLES20.glGetUniformLocation(mainProgram, "u_DiffuseColor")
        uSpecularColorHandle = GLES20.glGetUniformLocation(mainProgram, "u_SpecularColor")
        uShininessHandle = GLES20.glGetUniformLocation(mainProgram, "u_Shininess")
        uAlphaHandle = GLES20.glGetUniformLocation(mainProgram, "u_Alpha")

        aPositionHandle = GLES20.glGetAttribLocation(mainProgram, "a_Position")
        aNormalHandle = GLES20.glGetAttribLocation(mainProgram, "a_Normal")

        // Line Shader Program
        val lineVS = """
            uniform mat4 u_LineMVP;
            attribute vec4 a_LinePos;
            void main() {
                gl_Position = u_LineMVP * a_LinePos;
            }
        """.trimIndent()

        val lineFS = """
            precision mediump float;
            uniform vec4 u_LineColor;
            void main() {
                gl_FragColor = u_LineColor;
            }
        """.trimIndent()

        lineProgram = createProgram(lineVS, lineFS)
        uLineMVPMatrixHandle = GLES20.glGetUniformLocation(lineProgram, "u_LineMVP")
        uLineColorHandle = GLES20.glGetUniformLocation(lineProgram, "u_LineColor")
        aLinePositionHandle = GLES20.glGetAttribLocation(lineProgram, "a_LinePos")
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun createProgram(vsCode: String, fsCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vsCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fsCode)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vertexShader)
        GLES20.glAttachShader(prog, fragmentShader)
        GLES20.glLinkProgram(prog)
        return prog
    }
}
