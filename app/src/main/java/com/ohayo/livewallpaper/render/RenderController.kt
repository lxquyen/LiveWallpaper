/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ohayo.livewallpaper.render

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class ReloadType
object ReloadWhenVisible : ReloadType()
object ReloadDespiteInvisible : ReloadType()
object ReloadImmediate : ReloadType()

abstract class RenderController(
    protected var renderer: BlurRenderer,
    private var callbacks: Callbacks
) : DefaultLifecycleObserver {

    var visible: Boolean = false
        set(value) {
            field = value
            if (value) {
                callbacks.queueEventOnGlThread {
                    val loader = queuedImageLoader
                    if (loader != null) {
                        queuedImageLoader = null
                        renderer.setAndConsumeImageLoader(loader)
                    }
                }
                callbacks.requestRender()
            }
        }
    var onLockScreen: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                // Switch immediately if we're transitioning to the lock screen
                reloadCurrentArtwork(if (value) ReloadImmediate else ReloadDespiteInvisible)
            }
        }
    private lateinit var coroutineScope: CoroutineScope
    private var destroyed = false
    private var queuedImageLoader: ImageLoader? = null

    private val throttledForceReloadHandler by lazy {
        Handler(Looper.getMainLooper()) {
            reloadCurrentArtwork()
            true
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        coroutineScope = owner.lifecycleScope
    }

    override fun onDestroy(owner: LifecycleOwner) {
        queuedImageLoader = null
        destroyed = true
    }

    protected abstract suspend fun openDownloadedCurrentArtwork(): ImageLoader

    fun reloadCurrentArtwork(reloadType: ReloadType = ReloadWhenVisible) {
        if (destroyed) {
            // Don't reload artwork for destroyed RenderControllers
            return
        }
        coroutineScope.launch(Dispatchers.Main) {
            val imageLoader = openDownloadedCurrentArtwork()

            callbacks.queueEventOnGlThread {
                if (visible || reloadType != ReloadWhenVisible) {
                    renderer.setAndConsumeImageLoader(
                        imageLoader,
                        reloadType == ReloadImmediate || !visible
                    )
                } else {
                    queuedImageLoader = imageLoader
                }
            }
        }
    }

    interface Callbacks {
        fun queueEventOnGlThread(event: () -> Unit)
        fun requestRender()
    }
}
