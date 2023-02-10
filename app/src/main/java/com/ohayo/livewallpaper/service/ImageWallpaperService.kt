//package com.ohayo.livewallpaper.service
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.os.Build
//import android.view.SurfaceHolder
//import androidx.core.os.UserManagerCompat
//import androidx.lifecycle.DefaultLifecycleObserver
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.LifecycleOwner
//import androidx.lifecycle.LifecycleRegistry
//import com.ohayo.livewallpaper.render.BlurRenderer
//import com.ohayo.livewallpaper.render.RealRenderController
//import com.ohayo.livewallpaper.render.RenderController
//import net.rbgrn.android.glwallpaperservice.GLWallpaperService
//
///**
// * Created by Furuichi on 9/2/2023
// */
//class ImageWallpaperService : GLWallpaperService(), LifecycleOwner {
//
//    private val wallpaperLifecycle = LifecycleRegistry(this)
//    private var unlockReceiver: BroadcastReceiver? = null
//
//    override val lifecycle: Lifecycle = wallpaperLifecycle
//
//    override fun onCreateEngine(): Engine {
//        return ImageEngine()
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        if (UserManagerCompat.isUserUnlocked(this)) {
//            wallpaperLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            unlockReceiver = object : BroadcastReceiver() {
//                override fun onReceive(context: Context, intent: Intent) {
//                    wallpaperLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
//                    unregisterReceiver(this)
//                    unlockReceiver = null
//                }
//            }
//            val filter = IntentFilter(Intent.ACTION_USER_UNLOCKED)
//            registerReceiver(unlockReceiver, filter)
//        }
//    }
//
//    override fun onDestroy() {
//        if (unlockReceiver != null) {
//            unregisterReceiver(unlockReceiver)
//        }
//        wallpaperLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
//        super.onDestroy()
//    }
//
//    inner class ImageEngine : GLWallpaperService.GLEngine(),
//        LifecycleOwner,
//        DefaultLifecycleObserver,
//        BlurRenderer.Callbacks,
//        RenderController.Callbacks {
//
//        private lateinit var renderer: BlurRenderer
//        private lateinit var renderController: RenderController
//
//        private val engineLifecycle = LifecycleRegistry(this)
//
//        override fun onCreate(surfaceHolder: SurfaceHolder?) {
//            super<GLEngine>.onCreate(surfaceHolder)
//            renderer = BlurRenderer(
//                context = this@ImageWallpaperService, callbacks = this,
//                demoMode = false,
//                isPreview = isPreview
//            )
//            renderController = RealRenderController(
//                context = this@ImageWallpaperService,
//                renderer = renderer,
//                callbacks = this
//            )
//            engineLifecycle.addObserver(renderController)
//            setEGLContextClientVersion(2)
//            setEGLConfigChooser(8, 8, 8, 0, 0, 0)
//            setRenderer(renderer)
//            renderMode = RENDERMODE_WHEN_DIRTY
//            requestRender()
//
//            engineLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
//        }
//
//        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
//            super.onSurfaceChanged(holder, format, width, height)
//            renderController.reloadCurrentArtwork()
//        }
//
//        override fun onDestroy() {
//            wallpaperLifecycle.removeObserver(this)
//            engineLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
//            queueEvent {
//                renderer.destroy()
//            }
//            super<GLEngine>.onDestroy()
//        }
//
//        override fun onVisibilityChanged(visible: Boolean) {
//            renderController.visible = visible
//        }
//
//        override fun onOffsetsChanged(
//            xOffset: Float,
//            yOffset: Float,
//            xOffsetStep: Float,
//            yOffsetStep: Float,
//            xPixelOffset: Int,
//            yPixelOffset: Int
//        ) {
//            super.onOffsetsChanged(
//                xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset,
//                yPixelOffset
//            )
//            renderer.setNormalOffsetX(xOffset)
//        }
//
//        override fun onZoomChanged(zoom: Float) {
//            super.onZoomChanged(zoom)
//            renderer.setZoom(zoom)
//        }
//
//        override fun queueEventOnGlThread(event: () -> Unit) {
//            queueEvent {
//                event()
//            }
//        }
//
//        override val lifecycle: Lifecycle = engineLifecycle
//
//        override fun onStart(owner: LifecycleOwner) {
//            // The MuzeiWallpaperService only gets to ON_START when the user is unlocked
//            // At that point, we can proceed with the engine's lifecycle
//            // In preview mode, we only move to ON_START to avoid analytics events.
//            engineLifecycle.handleLifecycleEvent(
//                if (isPreview)
//                    Lifecycle.Event.ON_START else Lifecycle.Event.ON_RESUME
//            )
//        }
//    }
//
//
//}