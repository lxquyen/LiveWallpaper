package com.ohayo.livewallpaper

/**
 * Created by Furuichi on 04/02/2023
 */
data class Wallpaper(
    val name: String,
    val description: String,
    val fileName: String,
) {
    companion object {
        val wallpapers: List<Wallpaper> by lazy {
            return@lazy listOf(
                Wallpaper(
                    "Demo",
                    "Demo description",
                    "demo.mp4"
                ),
                Wallpaper(
                    "Demo2",
                    "Demo2 description",
                    "demo2.mp4"
                ),
                Wallpaper(
                    "Demo3",
                    "Demo3 description",
                    "demo3.mp4"
                ),
                Wallpaper(
                    "Demo4",
                    "Demo4 description",
                    "demo4.mp4"
                ),
                Wallpaper(
                    "Demo5",
                    "Demo5 description",
                    "demo5.mp4"
                )
            )
        }
    }
}