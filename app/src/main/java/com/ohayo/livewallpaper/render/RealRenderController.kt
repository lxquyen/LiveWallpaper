package com.ohayo.livewallpaper.render

import android.content.Context
import androidx.lifecycle.LifecycleOwner

/**
 * Created by Furuichi on 10/2/2023
 */
class RealRenderController(
    private val context: Context,
    renderer: BlurRenderer,
    callbacks: Callbacks
) : RenderController(renderer, callbacks) {

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        reloadCurrentArtwork()
    }

    override suspend fun openDownloadedCurrentArtwork() = AssetImageLoader(context.assets, "starrynight.jpg")
}