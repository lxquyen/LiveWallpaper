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

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.ohayo.livewallpaper.Utils

class RealRenderController(
    context: Context,
    renderer: MuzeiBlurRenderer,
    callbacks: Callbacks
) : RenderController(context, renderer, callbacks) {

    private val images = listOf(
        "demo-image-01.jpg",
        "demo-image-02.jpg",
        "demo-image-03.jpg",
        "demo-image-04.jpg",
        "demo-image-05.jpg"
    )

    private var index = 0

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        reloadCurrentArtwork()
        renderer.setIsBlurred(isBlurred = false, artDetailMode = false)
    }

    override suspend fun openDownloadedCurrentArtwork(): ImageLoader {
        val loader = AssetImageLoader(context.assets, images[index])
        Utils.logD("RealRenderController", "index = $index")
        return loader
    }

    override fun prepareReloadCurrentArtwork() {
        index = (++index) % images.size
    }
}
