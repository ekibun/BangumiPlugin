package soko.ekibun.bangumi.plugins.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat


object NotificationUtil{
    //创建渠道并发布通知
    fun builder(context: Context, channelId: String, title: String): NotificationCompat.Builder {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(context, channelId)
    }
}