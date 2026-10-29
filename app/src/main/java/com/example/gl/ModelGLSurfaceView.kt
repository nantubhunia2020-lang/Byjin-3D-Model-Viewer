package com.example.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.hypot

class ModelGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    val renderer = ModelRenderer()

    private var previousX = 0f
    private var previousY = 0f
    private var previousPointerX = 0f
    private var previousPointerY = 0f
    private var isPanning = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            if (scaleFactor > 0.01f) {
                renderer.zoom(scaleFactor)
                requestRender()
            }
            return true
        }
    })

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY // Continuous for smooth turntable/interactions
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        val pointerCount = event.pointerCount

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                previousX = event.x
                previousY = event.y
                isPanning = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (pointerCount == 2) {
                    isPanning = true
                    previousPointerX = (event.getX(0) + event.getX(1)) / 2f
                    previousPointerY = (event.getY(0) + event.getY(1)) / 2f
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    if (pointerCount == 1 && !isPanning) {
                        // 1-finger Orbit
                        val dx = event.x - previousX
                        val dy = event.y - previousY
                        renderer.orbit(dx, dy)
                        requestRender()
                    } else if (pointerCount >= 2) {
                        // 2-finger Pan
                        val midX = (event.getX(0) + event.getX(1)) / 2f
                        val midY = (event.getY(0) + event.getY(1)) / 2f
                        val dx = midX - previousPointerX
                        val dy = midY - previousPointerY
                        renderer.pan(dx, dy)
                        previousPointerX = midX
                        previousPointerY = midY
                        requestRender()
                    }
                }
                previousX = event.x
                previousY = event.y
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 2) {
                    isPanning = false
                    val remainingIndex = if (event.actionIndex == 0) 1 else 0
                    previousX = event.getX(remainingIndex)
                    previousY = event.getY(remainingIndex)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPanning = false
            }
        }
        return true
    }
}
