package soko.ekibun.bangumi.plugins.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.EpisodeCache
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.util.AppUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil
import soko.ekibun.bangumi.plugins.util.NotificationUtil

class DownloadService(val app: Context, val pluginContext: Context) {
    private val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val taskCollection = HashMap<String, DownloadTask>()

    private fun getTaskKey(episode: Episode, subject: Subject): String {
        return subject.prefKey + "_${episode.manga?.id ?: episode.id}"
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
                    task.cancel(true)
                    sendBroadcast(episode, subject, task.cache, false)
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
                            .setContentText(task.cache.cache()?.getProgressInfo() ?: "")
                            .setContentIntent(pIntent).build()
                    )
                } else {
                    val cache = JsonUtil.toEntity<EpisodeCache>(request.getStringExtra(EXTRA_CACHE) ?: "") ?: return
                    val newTask = DownloadTask(cache) { mTask: DownloadTask ->
                        App.app.episodeCacheModel.addEpisodeCache(subject, cache)
                        val status = taskCollection.filter { mTask.cache.cache()?.isFinished() != true }.size
                        val isFinished = mTask.cache.cache()?.isFinished() == true
                        if (isFinished) taskCollection.remove(taskKey)

                        sendBroadcast(episode, subject, mTask.cache, true)
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
                            .setContentText(mTask.cache.cache()?.getProgressInfo() ?: "")
                            .also {
                                if (!isFinished) it.setProgress(
                                    10000,
                                    ((mTask.cache.cache()?.getProgress() ?: 0f) * 10000).toInt(),
                                    mTask.cache.cache()?.getProgress() ?: 0f < 1e-10
                                )
                            }
                            .setContentIntent(pIntent).build())
                    }
                    taskCollection[taskKey] = newTask
                    newTask.executeOnExecutor(App.cachedThreadPool)
                }
            }
            ACTION_REMOVE -> {
                manager.cancel(taskKey, 0)
                if (taskCollection.containsKey(taskKey)) {
                    taskCollection[taskKey]!!.cancel(true)
                    taskCollection.remove(taskKey)
                }
                if (taskCollection.isEmpty())
                    manager.cancel(0)

                val videoCache = App.app.episodeCacheModel.getEpisodeCache(episode, subject) ?: return
                sendBroadcast(episode, subject, null, false)
                videoCache.remove()
                App.app.episodeCacheModel.removeEpisodeCache(episode, subject)
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

    class DownloadTask(val cache: EpisodeCache, val update: (DownloadTask) -> Unit) : AsyncTask<Unit, Unit, Unit>() {
        override fun onProgressUpdate(vararg values: Unit?) {
            update(this)
            super.onProgressUpdate(*values)
        }

        override fun onPostExecute(result: Unit?) {
            if (!isCancelled) update(this)
            super.onPostExecute(result)
        }

        override fun doInBackground(vararg params: Unit?) {
            val cacheCache = cache.cache() ?: return
            publishProgress()
            while (!Thread.currentThread().isInterrupted && !cacheCache.isFinished()) {
                try {
                    if (!cacheCache.download {
                            cache.cache = JsonUtil.toJson(cacheCache)
                            publishProgress()
                        }) break
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                    Thread.sleep(1000)
                }
                cache.cache = JsonUtil.toJson(cacheCache)
            }
            Thread.sleep(100)
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
