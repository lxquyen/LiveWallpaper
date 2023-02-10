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

class RealRenderController(
    context: Context,
    renderer: MuzeiBlurRenderer,
    callbacks: Callbacks
) : RenderController(context, renderer, callbacks) {

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        reloadCurrentArtwork()
    }

    override suspend fun openDownloadedCurrentArtwork() =
        AssetImageLoader(context.assets, "starrynight.jpg")
}
