package soko.ekibun.bangumi.plugins.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.model.VideoModel
import soko.ekibun.bangumi.plugins.provider.music.MusicPluginView
import soko.ekibun.bangumi.plugins.util.AppUtil
import soko.ekibun.bangumi.plugins.util.GlideUtil
import soko.ekibun.bangumi.plugins.util.NotificationUtil

class MusicService : Service() {
    private val manager by lazy { this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private fun getMusicNotification(): Notification {
        if (VideoModel.cover == null) App.mainScope.launch(Dispatchers.IO) {
            try {
                VideoModel.cover = GlideUtil.with(App.app.host)?.asBitmap()
                    ?.load(VideoModel.lastState?.pluginView?.linePresenter?.subject?.image)?.submit()?.get()
            } catch (e: Throwable) {
            }
            if (VideoModel.cover != null) updateNotification()
        }

        return NotificationUtil.builder(App.app.plugin, "music", "音乐")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentText(VideoModel.lastState?.pluginView?.linePresenter?.subject?.displayName)
            .setContentTitle(VideoModel.lastState?.data?.episode?.displayName)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(1, 2, 3))
            .setLargeIcon(VideoModel.cover)
            .setContentIntent(VideoModel.lastState?.pluginView?.linePresenter?.subject?.let {
                PendingIntent.getActivity(
                    App.app.host,
                    2,
                    AppUtil.parseSubjectActivityIntent(it),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            })
            .addAction(
                MusicPluginView.repeatList[VideoModel.lastState?.pluginView?.repeat ?: 0], "循环",
                PendingIntent.getBroadcast(
                    App.app.host,
                    VideoModel.CONTROL_TYPE_REPEAT,
                    Intent(VideoModel.ACTION_MEDIA_CONTROL + VideoModel.lastState?.pluginView?.linePresenter?.subject?.id).putExtra(
                        VideoModel.EXTRA_CONTROL_TYPE,
                        VideoModel.CONTROL_TYPE_REPEAT
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .addAction(
                R.drawable.ic_prev, "上一集",
                PendingIntent.getBroadcast(
                    App.app.host,
                    VideoModel.CONTROL_TYPE_PREV,
                    Intent(VideoModel.ACTION_MEDIA_CONTROL + VideoModel.lastState?.pluginView?.linePresenter?.subject?.id).putExtra(
                        VideoModel.EXTRA_CONTROL_TYPE,
                        VideoModel.CONTROL_TYPE_PREV
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .addAction(
                if (!VideoModel.player.playWhenReady) R.drawable.ic_play else R.drawable.ic_pause,
                if (!VideoModel.player.playWhenReady) "播放" else "暂停",
                PendingIntent.getBroadcast(
                    App.app.host,
                    VideoModel.CONTROL_TYPE_PLAY,
                    Intent(VideoModel.ACTION_MEDIA_CONTROL + VideoModel.lastState?.pluginView?.linePresenter?.subject?.id).putExtra(
                        VideoModel.EXTRA_CONTROL_TYPE,
                        if (!VideoModel.player.playWhenReady) VideoModel.CONTROL_TYPE_PLAY else VideoModel.CONTROL_TYPE_PAUSE
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .addAction(
                R.drawable.ic_next, "下一集",
                PendingIntent.getBroadcast(
                    App.app.host,
                    VideoModel.CONTROL_TYPE_NEXT,
                    Intent(VideoModel.ACTION_MEDIA_CONTROL + VideoModel.lastState?.pluginView?.linePresenter?.subject?.id).putExtra(
                        VideoModel.EXTRA_CONTROL_TYPE,
                        VideoModel.CONTROL_TYPE_NEXT
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (VideoModel.player.isPlaying) startForeground(2, getMusicNotification())
        else {
            stopForeground(false)
            manager.notify(2, getMusicNotification())
        }
        return super.onStartCommand(intent, flags, startId)
    }


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        fun updateNotification() {
            App.app.plugin.startService(Intent(App.app.plugin, MusicService::class.java))
        }
    }
}
