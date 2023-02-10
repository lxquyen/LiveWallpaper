///*
// * Copyright 2014 Google Inc.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.ohayo.livewallpaper.render
//
//import android.app.ActivityManager
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.RectF
//import android.opengl.GLES20
//import android.opengl.GLSurfaceView
//import android.opengl.Matrix
//import android.util.Log
//import android.view.animation.AccelerateDecelerateInterpolator
//import androidx.annotation.Keep
//import androidx.lifecycle.DefaultLifecycleObserver
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.LifecycleOwner
//import com.ohayo.livewallpaper.util.constrain
//import com.ohayo.livewallpaper.util.floorEven
//import com.ohayo.livewallpaper.util.interpolate
//import com.ohayo.livewallpaper.util.roundMult4
//import javax.microedition.khronos.egl.EGLConfig
//import javax.microedition.khronos.opengles.GL10
//import kotlin.math.max
//import kotlin.math.min
//import kotlin.math.sqrt
//
//
//class BlurRenderer(
//    context: Context,
//    private val callbacks: Callbacks,
//    private val demoMode: Boolean = false,
//    isPreview: Boolean,
//) : GLSurfaceView.Renderer, LifecycleOwner, DefaultLifecycleObserver {
//
//    companion object {
//        private const val TAG = "BlurRenderer"
//        const val DEFAULT_MAX_DIM = 128 // technical max 255
//        private const val DEMO_DIM = 64
//        private const val DIM_RANGE = 0.5f // percent of max dim
//    }
//
//    private val blurKeyframes: Int
//    private var maxPrescaledBlurPixels: Int = 0
//    private var blurredSampleSize: Int = 0
//    private var maxDim: Int = 0
//    private var maxGrey: Int = 0
//
//    // Model and view matrices. Projection and MVP stored in picture set
//    private val modelMatrix = FloatArray(16)
//    private val viewMatrix = FloatArray(16)
//
//    private var aspectRatio: Float = 0f
//    private var currentHeight: Int = 0
//
//    private var currentGLPictureSet: GLPictureSet
//    private var nextGLPictureSet: GLPictureSet
//
//    private var queuedNextImageLoader: ImageLoader? = null
//
//    private var surfaceCreated: Boolean = false
//
//    @Volatile
//    private var normalOffsetX: Float = 0f
//
//    @Volatile
//    private var zoomAmount: Float = 1f
//    private val currentViewport = RectF() // [-1, -1] to [1, 1], flipped
//
//    private var blurRelatedToArtDetailMode = false
//    private val blurInterpolator = AccelerateDecelerateInterpolator()
//
//    init {
//        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//        blurKeyframes = if (activityManager.isLowRamDevice) 1 else 2
//
//        currentGLPictureSet = GLPictureSet(0)
//        nextGLPictureSet = GLPictureSet(1) // for transitioning to next pictures
//        setNormalOffsetX(0f)
//        setZoom(1f)
//    }
//
//    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
//        surfaceCreated = false
//        GLES20.glEnable(GLES20.GL_BLEND)
//        //        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//        GLES20.glBlendFuncSeparate(
//            GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA,
//            GLES20.GL_ONE, GLES20.GL_ONE
//        )
//        GLES20.glClearColor(0f, 0f, 0f, 0f)
//
//        // Set the camera position (View matrix)
//        Matrix.setLookAtM(
//            viewMatrix, 0,
//            0f, 0f, 1f,
//            0f, 0f, -1f,
//            0f, 1f, 0f
//        )
//
//        surfaceCreated = true
//        val loader = queuedNextImageLoader
//        if (loader != null) {
//            queuedNextImageLoader = null
//            setAndConsumeImageLoader(loader)
//        }
//    }
//
//    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
//        GLES20.glViewport(0, 0, width, height)
//        hintViewportSize(width, height)
//        currentGLPictureSet.recomputeTransformMatrices()
//        nextGLPictureSet.recomputeTransformMatrices()
//    }
//
//    fun hintViewportSize(width: Int, height: Int) {
//        currentHeight = height
//        aspectRatio = width * 1f / height
//    }
//
//    override fun onDrawFrame(unused: GL10) {
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//
//        Matrix.setIdentityM(modelMatrix, 0)
//
//        if (blurRelatedToArtDetailMode) {
//            currentGLPictureSet.recomputeTransformMatrices()
//            nextGLPictureSet.recomputeTransformMatrices()
//        }
//
//        currentGLPictureSet.drawFrame()
//    }
//
//    @Keep
//    fun setNormalOffsetX(x: Float) {
//        normalOffsetX = x.constrain(0f, 1f)
//        onViewportChanged()
//    }
//
//    fun setZoom(zoom: Float) {
//        zoomAmount = interpolate(1f, 1.1f, 1 - zoom.constrain(0f, 1f))
//        onViewportChanged()
//    }
//
//    private fun onViewportChanged() {
//        currentGLPictureSet.recomputeTransformMatrices()
//        nextGLPictureSet.recomputeTransformMatrices()
//        if (surfaceCreated) {
//            callbacks.requestRender()
//        }
//    }
//
//    private fun blurRadiusAtFrame(f: Float): Float {
//        return maxPrescaledBlurPixels * blurInterpolator.getInterpolation(f / blurKeyframes)
//    }
//
//    fun setAndConsumeImageLoader(imageLoader: ImageLoader, immediate: Boolean = false) {
//        if (!surfaceCreated) {
//            queuedNextImageLoader = imageLoader
//            return
//        }
//
//        val (width, height) = imageLoader.getSize()
//        if (width == 0 || height == 0) {
//            return
//        }
//        nextGLPictureSet.load(imageLoader)
//        callbacks.requestRender()
//    }
//
//    private inner class GLPictureSet(val id: Int) {
//        private val projectionMatrix = FloatArray(16)
//        private val mvpMatrix = FloatArray(16)
//        private val pictures = arrayOfNulls<GLPicture>(blurKeyframes + 1)
//        private var hasBitmap = false
//        private var bitmapAspectRatio = 1f
//        var dimAmount = 0
//
//        fun load(imageLoader: ImageLoader) {
//            val (width, height) = imageLoader.getSize()
//            hasBitmap = width != 0 && height != 0
//            bitmapAspectRatio = if (hasBitmap)
//                width * 1f / height
//            else
//                1f
//
//            dimAmount = DEFAULT_MAX_DIM
//
//            destroyPictures()
//
//            if (hasBitmap) {
//                // Calculate image darkness to determine dim amount
//                var tempBitmap = imageLoader.decode(64)
//                val darkness = tempBitmap.darkness()
//                dimAmount = if (demoMode)
//                    DEMO_DIM
//                else
//                    (maxDim * (1 - DIM_RANGE + DIM_RANGE * sqrt(darkness.toDouble()))).toInt()
//                tempBitmap?.recycle()
//
//                // Create the GLPicture objects
//                var success = false
//                var sampleSize = 1
//                do {
//                    val attemptedWidth = (bitmapAspectRatio * currentHeight / sampleSize).toInt()
//                    val attemptedHeight = currentHeight / sampleSize
//                    try {
//                        val image = imageLoader.decode(
//                            attemptedWidth,
//                            attemptedHeight
//                        )
//                        pictures[0] = image?.toGLPicture()
//                        success = true
//                    } catch (e: OutOfMemoryError) {
//                        sampleSize = sampleSize shl 1
//                        Log.d(
//                            TAG, "Decoding image at ${attemptedWidth}x$attemptedHeight " +
//                                    "was too large, trying a sample size of $sampleSize"
//                        )
//                    }
//                } while (!success)
//                if (maxPrescaledBlurPixels == 0 && maxGrey == 0) {
//                    for (f in 1..blurKeyframes) {
//                        pictures[f] = pictures[0]
//                    }
//                } else {
//                    val sampleSizeTargetHeight: Int = if (maxPrescaledBlurPixels > 0) {
//                        currentHeight / blurredSampleSize
//                    } else {
//                        currentHeight
//                    }
//                    // Note that image width should be a multiple of 4 to avoid
//                    // issues with RenderScript allocations.
//                    val scaledHeight = max(2, sampleSizeTargetHeight.floorEven())
//                    val scaledWidth = max(4, (scaledHeight * bitmapAspectRatio).toInt().roundMult4())
//
//                    // To blur, first load the entire bitmap region, but at a very large
//                    // sample size that's appropriate for the final blurred image
//                    tempBitmap = imageLoader.decode(scaledWidth, scaledHeight)
//
//                    if (tempBitmap != null
//                        && tempBitmap.width != 0 && tempBitmap.height != 0
//                    ) {
//                        // Next, create a scaled down version of the bitmap so that the blur radius
//                        // looks appropriate (tempBitmap will likely be bigger than the final
//                        // blurred bitmap, and thus the blur may look smaller if we just used
//                        // tempBitmap as the final blurred bitmap).
//
//                        // Note that image width should be a multiple of 4 to avoid
//                        // issues with RenderScript allocations.
//                        val scaledBitmap = Bitmap.createScaledBitmap(
//                            tempBitmap, scaledWidth, scaledHeight, true
//                        )
//                        if (tempBitmap != scaledBitmap) {
//                            tempBitmap.recycle()
//                        }
//
//                        // And finally, create a blurred copy for each keyframe.
//
//                        scaledBitmap.recycle()
//                    } else {
//                        Log.e(TAG, "ImageLoader failed to decode the image")
//                    }
//                }
//            }
//
//            recomputeTransformMatrices()
//            callbacks.requestRender()
//        }
//
//        fun recomputeTransformMatrices() {
//            val screenToBitmapAspectRatio = aspectRatio / bitmapAspectRatio
//            if (screenToBitmapAspectRatio == 0f) {
//                return
//            }
//
//            // Ensure the bitmap is as wide as the screen by applying zoom if necessary
//            // ignoring any system wide zoom requests while the Art Detail screen is open
//            val zoom = zoomAmount
//
//            // Total scale factors in both zoom and scale due to aspect ratio.
//            val scaledBitmapToScreenAspectRatio = zoom / screenToBitmapAspectRatio
//
//            // At most pan across 1.8 screenfuls (2 screenfuls + some parallax)
//            // TODO: if we know the number of home screen pages, use that number here
//            val maxPanScreenWidths = min(1.8f, scaledBitmapToScreenAspectRatio)
//
//            currentViewport.apply {
//                left = interpolate(
//                    -1f, 1f,
//                    interpolate(
//                        (1 - maxPanScreenWidths / scaledBitmapToScreenAspectRatio) / 2,
//                        (1 + (maxPanScreenWidths - 2) / scaledBitmapToScreenAspectRatio) / 2,
//                        normalOffsetX
//                    )
//                )
//                right = left + 2f / scaledBitmapToScreenAspectRatio
//                bottom = -1f / zoom
//                top = 1f / zoom
//            }
//
//            Matrix.orthoM(
//                projectionMatrix, 0,
//                currentViewport.left, currentViewport.right,
//                currentViewport.bottom, currentViewport.top,
//                1f, 10f
//            )
//        }
//
//        fun drawFrame() {
//            if (!hasBitmap) {
//                return
//            }
//
//            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
//            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
//        }
//
//        fun destroyPictures() {
//            for (i in pictures.indices) {
//                if (pictures[i] != null) {
//                    pictures[i]?.destroy()
//                    pictures[i] = null
//                }
//            }
//        }
//    }
//
//    fun destroy() {
//        currentGLPictureSet.destroyPictures()
//        nextGLPictureSet.destroyPictures()
//    }
//
//    fun interface Callbacks {
//        fun requestRender()
//    }
//
//    override val lifecycle: Lifecycle
//        get() = TODO("Not yet implemented")
//}