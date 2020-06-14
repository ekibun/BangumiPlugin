package soko.ekibun.bangumi.plugins.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.model.EpisodeCacheModel
import soko.ekibun.bangumi.plugins.model.cache.EpisodeCache
import soko.ekibun.bangumi.plugins.util.AppUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil
import soko.ekibun.bangumi.plugins.util.NotificationUtil

class DownloadService : Service(), CoroutineScope by MainScope() {
    private val manager by lazy { this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val taskCollection = HashMap<String, Pair<EpisodeCache, Job>>()

    private fun getTaskKey(episode: Episode, subject: Subject): String {
        return subject.prefKey + "_${episode.provider?.id ?: episode.id}"
    }

    private fun getGroupSummary(status: Int): String {
        val groupKey = "download"
        manager.notify(
            0, NotificationUtil.builder(this, downloadChannelId, "下载")
                .setSmallIcon(
                    when (status) {
                        0 -> R.drawable.offline_pin
                        -1 -> R.drawable.ic_pause
                        else -> android.R.drawable.stat_sys_download
                    }
                )
                .setContentTitle("")
                .setAutoCancel(true)
                .setGroupSummary(true)
                .setGroup(groupKey)
                .build()
        )
        return groupKey
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val request = intent ?: return super.onStartCommand(intent, flags, startId)
        val episode =
            JsonUtil.toEntity<Episode>(request.getStringExtra(EXTRA_EPISODE) ?: "") ?: return super.onStartCommand(
                intent,
                flags,
                startId
            )
        val subject =
            JsonUtil.toEntity<Subject>(request.getStringExtra(EXTRA_SUBJECT) ?: "") ?: return super.onStartCommand(
                intent,
                flags,
                startId
            )
        val taskKey = getTaskKey(episode, subject)
        when (request.action) {
            ACTION_DOWNLOAD -> {
                val task = taskCollection[taskKey]
                if (task != null) {
                    taskCollection.remove(taskKey)
                    task.second.cancel()
                    sendBroadcast(episode, subject, task.first, false)
                    val pIntent = PendingIntent.getActivity(
                        this, taskKey.hashCode(),
                        AppUtil.parseSubjectActivityIntent(subject), PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    manager.notify(
                        taskKey, 0, NotificationUtil.builder(this, downloadChannelId, "下载")
                            .setSmallIcon(R.drawable.ic_pause)
                            .setOngoing(false)
                            .setAutoCancel(true)
                            .setGroup(this@DownloadService.getGroupSummary(-1))
                            .setContentTitle("已暂停 ${subject.name} ${episode.parseSort(this)}")
                            .setContentText(task.first.cache()?.getProgressInfo() ?: "")
                            .setContentIntent(pIntent).build()
                    )
                } else {
                    val cache = JsonUtil.toEntity<EpisodeCache>(request.getStringExtra(EXTRA_CACHE) ?: "")
                        ?: return super.onStartCommand(intent, flags, startId)
                    taskCollection[taskKey] =
                        cache to launch {
                            try {
                                createDownloadTask(cache) { epCache ->
                                    EpisodeCacheModel.addEpisodeCache(subject, cache)
                                    val status = taskCollection.filter { epCache.cache()?.isFinished() != true }.size
                                    val isFinished = epCache.cache()?.isFinished() == true

                                    sendBroadcast(episode, subject, epCache, true)
                                    val pIntent = PendingIntent.getActivity(
                                        this@DownloadService, taskKey.hashCode(),
                                        AppUtil.parseSubjectActivityIntent(subject), PendingIntent.FLAG_UPDATE_CURRENT
                                    )
                                    manager.notify(
                                        taskKey,
                                        0,
                                        NotificationUtil.builder(this@DownloadService, downloadChannelId, "下载")
                                            .setSmallIcon(if (isFinished) R.drawable.offline_pin else android.R.drawable.stat_sys_download)
                                            .setOngoing(!isFinished)
                                            .setAutoCancel(true)
                                            .setGroup(this@DownloadService.getGroupSummary(status))
                                            .setContentTitle(
                                                (if (isFinished) "已完成 " else "") + "${subject.name} ${episode.parseSort(
                                                    this@DownloadService
                                                )}"
                                            )
                                            .setContentText(epCache.cache()?.getProgressInfo() ?: "")
                                        .also {
                                            if (!isFinished) it.setProgress(
                                                10000,
                                                ((epCache.cache()?.getProgress() ?: 0f) * 10000).toInt(),
                                                epCache.cache()?.getProgress() ?: 0f < 1e-10
                                            )
                                        }
                                        .setContentIntent(pIntent).build())
                                }
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                            taskCollection.remove(taskKey)
                        }
                }
            }
            ACTION_REMOVE -> {
                manager.cancel(taskKey, 0)
                if (taskCollection.containsKey(taskKey)) {
                    taskCollection[taskKey]!!.second.cancel()
                    taskCollection.remove(taskKey)
                }
                if (taskCollection.isEmpty())
                    manager.cancel(0)

                launch {
                    val videoCache = EpisodeCacheModel.getEpisodeCache(episode, subject) ?: return@launch
                    sendBroadcast(episode, subject, null, false)
                    videoCache.remove()
                    EpisodeCacheModel.removeEpisodeCache(episode, subject)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun sendBroadcast(episode: Episode, subject: Subject, cache: EpisodeCache?, downloading: Boolean) {
        val broadcastIntent = Intent(getBroadcastAction(subject))
        broadcastIntent.putExtra(EXTRA_EPISODE, JsonUtil.toJson(episode))
        cache?.let { broadcastIntent.putExtra(EXTRA_CACHE, JsonUtil.toJson(cache)) }
        broadcastIntent.putExtra(EXTRA_DOWNLOADING, downloading)
        this.sendBroadcast(broadcastIntent)
    }

    private suspend fun createDownloadTask(cache: EpisodeCache, onUpdate: suspend (EpisodeCache) -> Unit) {
        withContext(Dispatchers.IO) {
            val cacheCache = cache.cache() ?: return@withContext
            withContext(Dispatchers.Main) { if (isActive) onUpdate(cache) }
            while (isActive && !cacheCache.isFinished()) {
                try {
                    if (!cacheCache.download {
                            cache.cache = JsonUtil.toJson(cacheCache)
                            launch(Dispatchers.Main) { if (isActive) onUpdate(cache) }
                        }) break
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(1000)
                }
            }
            cache.cache = JsonUtil.toJson(cacheCache)
            Log.v("cache", cacheCache.toString())
            withContext(Dispatchers.Main) { if (isActive) onUpdate(cache) }
        }
    }

    companion object {
        const val downloadChannelId = "download"

        const val EXTRA_EPISODE = "extraEpisode"
        const val EXTRA_SUBJECT = "extraSubject"
        const val EXTRA_CACHE = "extraCache"
        const val EXTRA_DOWNLOADING = "extraDownloading"

        const val ACTION_DOWNLOAD = "actionDownload"
        const val ACTION_REMOVE = "actionRemove"

        fun getBroadcastAction(subject: Subject): String {
            return "soko.ekibun.bangumi.plugin.download.${subject.prefKey}"
        }

        fun download(context: Context, episode: Episode, subject: Subject, cache: EpisodeCache) {
            val intent = Intent(context, DownloadService::class.java)
            intent.action = ACTION_DOWNLOAD
            intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
            intent.putExtra(EXTRA_EPISODE, JsonUtil.toJson(episode))
            intent.putExtra(EXTRA_CACHE, JsonUtil.toJson(cache))
            context.startService(intent)
        }

        fun remove(context: Context, episode: Episode, subject: Subject) {
            val intent = Intent(context, DownloadService::class.java)
            intent.action = ACTION_REMOVE
            intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
            intent.putExtra(EXTRA_EPISODE, JsonUtil.toJson(episode))
            context.startService(intent)
        }
    }
}
