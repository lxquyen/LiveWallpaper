package com.ohayo.livewallpaper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log

/**
 * Created by Furuichi on 03/02/2023
 */
object Utils {

    fun logD(tag: String, message: String) {
        Log.d(tag, message)
    }

    fun createNotificationChannel(notificationManager: NotificationManager, channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var notificationChannel =
                notificationManager.getNotificationChannel(channelId)
            if (notificationChannel == null) {
                notificationChannel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = channelName
                }
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }
    }

    fun getThumbnailOf(afd: AssetFileDescriptor, atTime: Int): Bitmap? {
        var bitmap: Bitmap? = null
        val retriever: MediaMetadataRetriever?
        if (afd.fileDescriptor.valid()) try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(atTime.toLong(), MediaMetadataRetriever.OPTION_CLOSEST, 1280, 720)
            } else {
                retriever.getFrameAtTime(atTime.toLong())
            }
            afd.close()
            retriever.release()
            Log.i("TAG", "getting bitmap process done")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmap
    }
}