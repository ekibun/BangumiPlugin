package soko.ekibun.bangumi.plugins.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.model.EpisodeCacheModel
import soko.ekibun.bangumi.plugins.model.cache.EpisodeCache
import soko.ekibun.bangumi.plugins.util.AppUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil
import soko.ekibun.bangumi.plugins.util.NotificationUtil

class DownloadService(val app: Context, val pluginContext: Context) {
    private val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val taskCollection = HashMap<String, Pair<EpisodeCache, Disposable>>()

    private fun getTaskKey(episode: Episode, subject: Subject): String {
        return subject.prefKey + "_${episode.book?.id ?: episode.id}"
    }

    private fun getGroupSummary(status: Int): String {
        val groupKey = "download"
        manager.notify(
            0, NotificationUtil.builder(app, downloadChannelId, "下载")
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

    fun onStartCommand(intent: Intent?, flags: Int, startId: Int) {
        val request = intent ?: return
        val episode = JsonUtil.toEntity<Episode>(request.getStringExtra(EXTRA_EPISODE) ?: "") ?: return
        val subject = JsonUtil.toEntity<Subject>(request.getStringExtra(EXTRA_SUBJECT) ?: "") ?: return
        val taskKey = getTaskKey(episode, subject)
        when (request.action) {
            ACTION_DOWNLOAD -> {
                val task = taskCollection[taskKey]
                if (task != null) {
                    taskCollection.remove(taskKey)
                    task.second.dispose()
                    sendBroadcast(episode, subject, task.first, false)
                    val pIntent = PendingIntent.getActivity(
                        app, taskKey.hashCode(),
                        AppUtil.parseSubjectActivityIntent(subject), PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    manager.notify(
                        taskKey, 0, NotificationUtil.builder(app, downloadChannelId, "下载")
                            .setSmallIcon(R.drawable.ic_pause)
                            .setOngoing(false)
                            .setAutoCancel(true)
                            .setGroup(this@DownloadService.getGroupSummary(-1))
                            .setContentTitle("已暂停 ${subject.name} ${episode.parseSort(pluginContext)}")
                            .setContentText(task.first.cache()?.getProgressInfo() ?: "")
                            .setContentIntent(pIntent).build()
                    )
                } else {
                    val cache = JsonUtil.toEntity<EpisodeCache>(request.getStringExtra(EXTRA_CACHE) ?: "") ?: return
                    taskCollection[taskKey] =
                        cache to createDownloadTask(cache).observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ epCache ->
                                EpisodeCacheModel.addEpisodeCache(subject, cache)
                                val status = taskCollection.filter { epCache.cache()?.isFinished() != true }.size
                                val isFinished = epCache.cache()?.isFinished() == true

                                sendBroadcast(episode, subject, epCache, true)
                                val pIntent = PendingIntent.getActivity(
                                    app, taskKey.hashCode(),
                                    AppUtil.parseSubjectActivityIntent(subject), PendingIntent.FLAG_UPDATE_CURRENT
                                )
                                manager.notify(taskKey, 0, NotificationUtil.builder(app, downloadChannelId, "下载")
                                    .setSmallIcon(if (isFinished) R.drawable.offline_pin else android.R.drawable.stat_sys_download)
                                    .setOngoing(!isFinished)
                                    .setAutoCancel(true)
                                    .setGroup(this@DownloadService.getGroupSummary(status))
                                    .setContentTitle(
                                        (if (isFinished) "已完成 " else "") + "${subject.name} ${episode.parseSort(
                                            pluginContext
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
                            }, {
                                it.printStackTrace()
                                taskCollection.remove(taskKey)
                            }, {
                                taskCollection.remove(taskKey)
                            })
                }
            }
            ACTION_REMOVE -> {
                manager.cancel(taskKey, 0)
                if (taskCollection.containsKey(taskKey)) {
                    taskCollection[taskKey]!!.second.dispose()
                    taskCollection.remove(taskKey)
                }
                if (taskCollection.isEmpty())
                    manager.cancel(0)

                val videoCache = EpisodeCacheModel.getEpisodeCache(episode, subject) ?: return
                sendBroadcast(episode, subject, null, false)
                videoCache.remove()
                EpisodeCacheModel.removeEpisodeCache(episode, subject)
            }
        }
    }

    private fun sendBroadcast(episode: Episode, subject: Subject, cache: EpisodeCache?, downloading: Boolean) {
        val broadcastIntent = Intent(getBroadcastAction(subject))
        broadcastIntent.putExtra(EXTRA_EPISODE, JsonUtil.toJson(episode))
        cache?.let { broadcastIntent.putExtra(EXTRA_CACHE, JsonUtil.toJson(cache)) }
        broadcastIntent.putExtra(EXTRA_DOWNLOADING, downloading)
        app.sendBroadcast(broadcastIntent)
    }

    private fun createDownloadTask(cache: EpisodeCache): Observable<EpisodeCache> {
        return Observable.create<EpisodeCache> { emitter ->
            val cacheCache = cache.cache() ?: return@create emitter.onComplete()
            if (!emitter.isDisposed) emitter.onNext(cache)
            while (!emitter.isDisposed && !cacheCache.isFinished()) {
                try {
                    if (!cacheCache.download {
                            cache.cache = JsonUtil.toJson(cacheCache)
                            if (!emitter.isDisposed) emitter.onNext(cache)
                        }) break
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                    Thread.sleep(1000)
                }
            }
            cache.cache = JsonUtil.toJson(cacheCache)
            Log.v("cache", cacheCache.toString())
            if (!emitter.isDisposed) {
                emitter.onNext(cache)
                emitter.onComplete()
            }
        }.subscribeOn(Schedulers.computation())
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

        val serviceComp = ComponentName("soko.ekibun.bangumi", "soko.ekibun.bangumi.RemoteService")

        fun download(context: Context, episode: Episode, subject: Subject, cache: EpisodeCache) {
            val intent = Intent()
            intent.component = serviceComp
            intent.action = ACTION_DOWNLOAD
            intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
            intent.putExtra(EXTRA_EPISODE, JsonUtil.toJson(episode))
            intent.putExtra(EXTRA_CACHE, JsonUtil.toJson(cache))
            context.startService(intent)
        }

        fun remove(context: Context, episode: Episode, subject: Subject) {
            val intent = Intent()
            intent.component = serviceComp
            intent.action = ACTION_REMOVE
            intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
            intent.putExtra(EXTRA_EPISODE, JsonUtil.toJson(episode))
            context.startService(intent)
        }
    }
}
