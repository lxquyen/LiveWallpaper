package com.ohayo.livewallpaper.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigPictureStyle
import com.ohayo.livewallpaper.R
import com.ohayo.livewallpaper.Utils
import com.ohayo.livewallpaper.Wallpaper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

/**
 * Created by Furuichi on 03/02/2023
 */
class VideoWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "CustomWallpaperService"
        private const val NOTIFICATION_ID = 99
        private const val CHANNEL_ID = "live_wallpaper"
        private const val CHANNEL_NAME = "Live Wallpaper"
        private const val ACTION_USER_PRESENT = "android.intent.action.USER_PRESENT"
        private const val ACTION_NOTIFICATION_NEXT = "com.ohayo.livewallpaper.NOTIFICATION.next"
        private const val ACTION_NOTIFICATION_INFO = "com.ohayo.livewallpaper.NOTIFICATION.info"

        private const val DEFAULT_INDEX = 0
    }

    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine(), CoroutineScope {

        private val notificationManager: NotificationManager by lazy {
            return@lazy getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        private val wallpapers: List<Wallpaper> = Wallpaper.wallpapers
        private var index = DEFAULT_INDEX
        private var mediaPlayer: MediaPlayer? = null
        private val nextTrigger = MutableSharedFlow<Unit>()
        private val jobCancel = Job()

        //Broadcast
        private val intentFilter: IntentFilter by lazy {
            return@lazy IntentFilter()
                .apply {
                    addAction(ACTION_USER_PRESENT)
                    addAction(ACTION_NOTIFICATION_NEXT)
                    addAction(ACTION_NOTIFICATION_INFO)
                }
        }
        private val broadcast = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_USER_PRESENT -> {
                        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                        if (keyguardManager.isKeyguardSecure) {
                            launch { nextTrigger.emit(Unit) }
                        }
                    }

                    ACTION_NOTIFICATION_NEXT -> {
                        launch { nextTrigger.emit(Unit) }
                    }

                    ACTION_NOTIFICATION_INFO -> {

                    }
                }

            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Utils.logD(TAG, "VideoEngine.onCreate")
            Utils.createNotificationChannel(notificationManager, CHANNEL_ID, CHANNEL_NAME)
            registerReceiver(broadcast, intentFilter)
        }

        @OptIn(FlowPreview::class)
        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Utils.logD(TAG, "VideoEngine.onSurfaceCreated")
            init(holder)

            nextTrigger
                .debounce(300)
                .onEach {
                    stop()
                    release()
                    init(holder)
                }
                .launchIn(this)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Utils.logD(TAG, "VideoEngine.onVisibilityChanged: $visible")
            if (visible) {
                mediaPlayer?.start()
            } else {
                mediaPlayer?.pause()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            Utils.logD(TAG, "VideoEngine.onSurfaceDestroyed")
            stop()
            release()
        }

        override fun onDestroy() {
            super.onDestroy()
            Utils.logD(TAG, "VideoEngine.onDestroy")
            unregisterReceiver(broadcast)
            release()
            index = DEFAULT_INDEX
            jobCancel.cancel()
        }

        private fun init(holder: SurfaceHolder) {
            val position = (index++) % wallpapers.size
            val wallpaper = wallpapers[position]
            val fileName = wallpaper.fileName
            Utils.logD(TAG, "File name = $fileName")
            val assetFileDes = resources.assets.openFd(fileName)
            mediaPlayer = MediaPlayer().apply {
                setSurface(holder.surface)
                setDataSource(
                    /* fd = */ assetFileDes.fileDescriptor,
                    /* offset = */ assetFileDes.startOffset,
                    /* length = */assetFileDes.length
                )
                isLooping = true
                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                prepare()
                start()
            }

            launch {
                val bitmap = Utils.getThumbnailOf(assetFileDes, 1)
                val notification = buildNotification(wallpaper, bitmap)
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }

        private fun stop() {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
        }

        private fun release() {
            mediaPlayer?.release()
            mediaPlayer = null
        }

        private fun buildNotification(wallpaper: Wallpaper, bitmap: Bitmap?): Notification {
            val nextPendingIntent = createPendingIntent(ACTION_NOTIFICATION_NEXT)
            val infoPendingIntent = createPendingIntent(ACTION_NOTIFICATION_INFO)

            val builder = NotificationCompat.Builder(this@VideoWallpaperService, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(wallpaper.name)
                .setContentText(wallpaper.fileName)
                .addAction(0, "Next", nextPendingIntent)
                .addAction(0, "Info", infoPendingIntent)

            bitmap?.also {
                builder.setLargeIcon(it)
                builder.setStyle(
                    BigPictureStyle()
                        .bigPicture(it)
                        .bigLargeIcon(null)
                )
            }

            return builder.build()
        }

        private fun createPendingIntent(action: String): PendingIntent? {
            val intent = Intent(action)
            return PendingIntent.getBroadcast(applicationContext, 1, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Main + jobCancel
    }
}

