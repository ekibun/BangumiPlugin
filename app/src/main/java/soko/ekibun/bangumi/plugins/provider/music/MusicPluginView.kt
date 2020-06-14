package soko.ekibun.bangumi.plugins.provider.music

import android.content.pm.ActivityInfo
import android.util.Log
import android.view.View
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.StreamKey
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.plugin_music.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.model.VideoModel
import soko.ekibun.bangumi.plugins.model.cache.EpisodeCache
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.service.DownloadService
import soko.ekibun.bangumi.plugins.service.MusicService
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.util.JsonUtil
import java.io.IOException
import java.util.*

class MusicPluginView(val linePresenter: LinePresenter) : Provider.PluginView(linePresenter, R.layout.plugin_music) {
    val sp = PreferenceManager.getDefaultSharedPreferences(App.app.plugin)

    companion object {
        const val PREF_REPEAT = "music_repeat"
        val repeatList = arrayOf(R.drawable.ic_repeat, R.drawable.ic_repeat_one, R.drawable.ic_shuffle)
    }

    val repeat get() = sp.getInt(PREF_REPEAT, 0)

    fun changeRepeat() {
        val repeat = (repeat + 1) % repeatList.size
        sp.edit().putInt(PREF_REPEAT, repeat).apply()
        controller.updateRepeat(repeatList[repeat])
    }

    private val controller: MusicController by lazy {
        MusicController(view, object : MusicController.OnActionListener {
            override fun onInfo() {
                showInfo(true)
            }

            override fun onNext() {
                nextEpisode()?.let { loadEp(it) }
            }

            override fun onPlayPause() {
                doPlayPause(!VideoModel.player.playWhenReady)
            }

            override fun onPrev() {
                prevEpisode()?.let { loadEp(it) }
            }

            override fun onRepeat() {
                changeRepeat()
            }

            override fun seekTo(pos: Long) {
                VideoModel.player.seekTo(pos)
                controller.updateProgress(VideoModel.player.currentPosition)
            }

        })
    }
    var endFlag = false
    private var inited = false
    var nextEpisode: () -> Episode? = { null }
    var prevEpisode: () -> Episode? = { null }

    /* access modifiers changed from: private */
    var playLoopTask: TimerTask? = null

    var startAt: Long? = null
    val timer: Timer = Timer()

    fun showInfo(show: Boolean) {
        linePresenter.proxy.subjectPresenter.subjectView.behavior.state =
            if (!show) BottomSheetBehavior.STATE_HIDDEN else BottomSheetBehavior.STATE_COLLAPSED
        linePresenter.proxy.item_mask.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    fun doPlayPause(play: Boolean) {
        VideoModel.player.playWhenReady = play
        updatePauseResume()
        playLoopTask?.cancel()
        if (play) {
            playLoopTask = object : TimerTask() {
                override fun run() {
                    linePresenter.activityRef.get()?.runOnUiThread {
                        updateProgress()
                        view.lrc_view.updateTime(VideoModel.player.currentPosition)
                    }
                }
            }
            timer.schedule(playLoopTask, 0, 100)
            view.lrc_view.keepScreenOn = true
            return
        } else view.lrc_view.keepScreenOn = false
    }

    /* access modifiers changed from: private */
    fun updateProgress() {
        linePresenter.activityRef.get()?.runOnUiThread {
            controller.duration = VideoModel.player.duration.toInt() / 10
            controller.buffedPosition = VideoModel.player.bufferedPosition.toInt() / 10
            controller.updateProgress(VideoModel.player.currentPosition)
        }
    }

    private fun updatePauseResume() {
        linePresenter.activityRef.get()?.runOnUiThread {
            controller.updatePauseResume(VideoModel.player.playWhenReady)
        }
    }

    private fun init() {
        if (!inited) {
            inited = true
            view.visibility = View.VISIBLE

            linePresenter.proxy.onStartListener = {
                VideoModel.attachToActivity(videoCallback)
            }
            linePresenter.proxy.subjectPresenter.subjectView.collapsibleAppBarHelper.let {
                val superTitleClick = it.onTitleClickListener
                it.onTitleClickListener = { ev ->
                    if (linePresenter.proxy.subjectPresenter.subjectView.behavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                        showInfo(true)
                    } else superTitleClick(ev)
                }
            }
            linePresenter.proxy.subjectPresenter.subjectView.let {
                it.behavior.isHideable = true
                it.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                it.peakRatio = 1 / 3f
            }
            linePresenter.proxy.onBackListener = {
                val behavior = linePresenter.proxy.subjectPresenter.subjectView.behavior
                if (behavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    showInfo(true)
                    linePresenter.activityRef.get()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    true
                } else false
            }
            linePresenter.proxy.subjectPresenter.subjectView.onStateChangedListener = onStateChangedListener@{ state ->
                controller.updatePrevNext(prevEpisode() != null, nextEpisode() != null)
                linePresenter.proxy.item_mask.visibility =
                    if (state == BottomSheetBehavior.STATE_HIDDEN) View.INVISIBLE else View.VISIBLE
            }
            linePresenter.proxy.item_plugin.setOnApplyWindowInsetsListener { _, insets ->
                view.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
                insets
            }
            linePresenter.proxy.item_plugin.requestApplyInsets()
            view.lrc_view.setDraggable(true) {
                VideoModel.player.seekTo(it)
                true
            }
            controller.updateRepeat(repeatList[repeat])
        }
    }

    override fun loadEp(episode: Episode) {
        if (linePresenter.subject.id != VideoModel.lastState?.pluginView?.linePresenter?.subject?.id) {
            VideoModel.cover = null
            VideoModel.player.stop(true)
        }
        loadEpImpl(episode, true)
    }

    private fun loadEpImpl(episode: Episode, play: Boolean) {
        val providerEpisode = episode.provider ?: return
        init()
        LineInfoModel.getInfo(linePresenter.subject).getDefaultProvider()?.let { info ->
            prevEpisode = {
                val curIndex = linePresenter.subject.eps?.indexOfFirst { Episode.compareEpisode(it, episode) } ?: 0
                linePresenter.subject.eps?.getOrNull(curIndex - 1)
            }
            nextEpisode = {
                val curIndex = linePresenter.subject.eps?.indexOfFirst { Episode.compareEpisode(it, episode) } ?: 0
                linePresenter.subject.eps?.getOrNull(curIndex + 1)
            }
            if (play) {
                startAt = null
                val scope = suspend scope@{
                    VideoModel.player.playWhenReady = false
                    linePresenter.proxy.subjectPresenter.subjectView.collapsibleAppBarHelper
                        .setTitle(null, providerEpisode.title, null)
                    playLoopTask?.cancel()
                    controller.updatePrevNext(prevEpisode() != null, nextEpisode() != null)
                    view.lrc_view.loadLrc("", "")
                    controller.updateProgress(0)
                    controller.updateLoading(true)
                    val data = VideoModel.getMusic("music", linePresenter.subject, episode, info)
                    val (request: Provider.HttpRequest?, streamKeys: List<StreamKey>?) = data
                    if (request == null) return@scope
                    val musicStoreData = MusicStoreData(episode)
                    VideoModel.lastState = VideoModel.StoreState(
                        this, musicStoreData
                    )
                    VideoModel.play(request, streamKeys = streamKeys)
                    MusicService.updateNotification()
                    val provider = LineProvider.getProvider(Provider.TYPE_MUSIC, info.site)?.provider as? MusicProvider
                        ?: return@scope
                    val lyric = provider.getLyric("lyric", providerEpisode)
                    view.lrc_view.loadLrc(lyric.lrc, lyric.tlyric)
                    musicStoreData.lyric = lyric
                }
                App.subscribe(key = "play") { scope() }
            }
        }
    }

    override fun downloadEp(episode: Episode, updateInfo: (String) -> Unit) {
        linePresenter.subscribe({
            updateInfo("解析出错：${it.message}")
        }, key = "download_ep_${episode.id}") {
            val subject = linePresenter.proxy.subjectPresenter.subject
            val info = LineInfoModel.getInfo(subject).getDefaultProvider() ?: throw IllegalStateException("请先添加播放源")
            updateInfo("获取音乐地址")
            val data = VideoModel.getMusic("music", linePresenter.subject, episode, info)
            val (request: Provider.HttpRequest?, _: List<StreamKey>?) = data
            if (request == null || request.url.startsWith("/")) return@subscribe
            updateInfo("创建音乐请求")
            VideoModel.createDownloadRequest(request, object : DownloadHelper.Callback {
                override fun onPrepared(helper: DownloadHelper) {
                    val downloadRequest = helper.getDownloadRequest(request.url, null)
                    DownloadService.download(
                        App.app.plugin, episode, subject,
                        EpisodeCache(
                            episode, Provider.TYPE_VIDEO, JsonUtil.toJson(
                                EpisodeCache.VideoCache(
                                    downloadRequest.type, downloadRequest.streamKeys, request
                                )
                            )
                        )
                    )
                }

                override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                    updateInfo(e.toString())
                }
            })
        }
    }

    private val videoCallback = object : VideoModel.Listener {
        override fun onReady(playWhenReady: Boolean) {
            if (playWhenReady) {
                doPlayPause(true)
                startAt?.let {
                    VideoModel.player.seekTo(it)
                    startAt = null
                }
            }
            controller.updateLoading(false)
            endFlag = true
        }

        override fun onBuffering() {
            controller.updateLoading(true)
        }

        override fun onEnded() {
            Log.v("end", "$endFlag $repeat")
            doPlayPause(false)
            if (endFlag) {
                endFlag = false
                when (repeatList[repeat]) {
                    R.drawable.ic_repeat -> {
                        (nextEpisode() ?: linePresenter.subject.eps?.firstOrNull { it.provider != null })?.let {
                            loadEp(
                                it
                            )
                        }
                    }
                    R.drawable.ic_shuffle -> {
                        val eps = linePresenter.subject.eps?.filterNot {
                            Episode.compareEpisode(
                                it,
                                VideoModel.lastState?.data?.episode
                            )
                        }
                        eps?.getOrNull((Math.random() * eps.size).toInt())?.let { loadEp(it) }
                    }
                }

            }
        }

        override fun onVideoSizeChange(
            width: Int,
            height: Int,
            unappliedRotationDegrees: Int,
            pixelWidthHeightRatio: Float
        ) {
        }

        override fun onError(error: ExoPlaybackException) {
//                showVideoError("视频加载错误\n${error.sourceException.localizedMessage}", "重新加载") {
//                    startAt = videoModel.player.currentPosition
//                    videoModel.reload()
//                }
        }
    }

    init {
        VideoModel.attachToActivity(videoCallback)
        if (VideoModel.lastState?.pluginView?.linePresenter?.subject?.id == linePresenter.subject.id) VideoModel.lastState?.data?.let { data ->
            loadEpImpl(data.episode, false)
            linePresenter.proxy.subjectPresenter.subjectView.collapsibleAppBarHelper
                .setTitle(null, data.episode.provider?.title, null)
            controller.updatePrevNext(prevEpisode() != null, nextEpisode() != null)
            data.lyric?.let { view.lrc_view.loadLrc(it.lrc, it.tlyric) }
            updateProgress()
            view.lrc_view.updateTime(VideoModel.player.currentPosition)
            doPlayPause(VideoModel.player.playWhenReady)
        }
    }

    data class MusicStoreData(
        val episode: Episode,
        var lyric: MusicProvider.Lyric? = null
    )
}