package com.ohayo.livewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.ohayo.livewallpaper.service.ImageWallpaperService
import com.ohayo.livewallpaper.service.VideoWallpaperService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn_execute_video).setOnClickListener {

            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, VideoWallpaperService::class.java)
                    )
                }
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_execute_image).setOnClickListener {

            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, ImageWallpaperService::class.java)
                    )
                }
            startActivity(intent)
        }
    }
}