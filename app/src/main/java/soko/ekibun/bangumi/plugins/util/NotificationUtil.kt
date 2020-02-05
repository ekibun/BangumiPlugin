package soko.ekibun.bangumi.plugins.util

import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.app.NotificationManager
import android.os.Build


object NotificationUtil{
    //创建渠道并发布通知
    fun builder(context: Context, channelId: String, title: String): Notification.Builder{
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        @Suppress("DEPRECATION")
        return if(Build.VERSION.SDK_INT >=26){
            val channel = NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
            Notification.Builder(context, channelId)
        }else
            Notification.Builder(context)
                    .setPriority(Notification.PRIORITY_LOW)
    }
}