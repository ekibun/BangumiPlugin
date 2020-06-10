package soko.ekibun.bangumi.plugins.provider.music

import android.content.pm.ActivityInfo
import android.view.View
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.plugin_music.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.model.VideoModel
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import java.util.*

class MusicPluginView(val linePresenter: LinePresenter) : Provider.PluginView(linePresenter, R.layout.plugin_music) {
    private val controller: MusicController by lazy {
        MusicController(view, object : MusicController.OnActionListener {
            override fun onInfo() {
                showInfo(true)
            }

            override fun onNext() {
                nextEpisode()?.let { loadEp(it) }
            }

            override fun onPlayPause() {
                doPlayPause(!videoModel.player.playWhenReady)
            }

            override fun onPrev() {
                prevEpisode()?.let { loadEp(it) }
            }

            override fun onRepeat() {
            }

            override fun seekTo(pos: Long) {
                videoModel.player.seekTo(pos)
                controller.updateProgress(videoModel.player.currentPosition)
            }

        })
    }
    var endFlag = false
    private var inited = false
    private var nextEpisode: () -> Episode? = { null }
    private var prevEpisode: () -> Episode? = { null }

    /* access modifiers changed from: private */
    var playLoopTask: TimerTask? = null

    var startAt: Long? = null
    val timer: Timer = Timer()

    val videoModel: VideoModel by lazy {
        VideoModel(linePresenter, object : VideoModel.Listener {
            override fun onReady(playWhenReady: Boolean) {
                if (playWhenReady) {
                    doPlayPause(true)
                    startAt?.let {
                        videoModel.player.seekTo(it)
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
                doPlayPause(false)
                if (endFlag) {
                    endFlag = false
                    nextEpisode()?.let { loadEp(it) }
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

        })
    }

    fun showInfo(show: Boolean) {
        linePresenter.proxy.subjectPresenter.subjectView.behavior.state =
            if (!show) BottomSheetBehavior.STATE_HIDDEN else BottomSheetBehavior.STATE_COLLAPSED
        linePresenter.proxy.item_mask.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    fun doPlayPause(play: Boolean) {
        videoModel.player.playWhenReady = play
        updatePauseResume()
        playLoopTask?.cancel()
        if (play) {
            playLoopTask = object : TimerTask() {
                override fun run() {
                    linePresenter.activityRef.get()?.runOnUiThread {
                        updateProgress()
                        view.lrc_view.updateTime(videoModel.player.currentPosition)
                    }
                }
            }
            timer.schedule(playLoopTask, 0, 100)
            view.lrc_view.keepScreenOn = true
            return
        }
        view.lrc_view.keepScreenOn = false
    }

    /* access modifiers changed from: private */
    fun updateProgress() {
        linePresenter.activityRef.get()?.runOnUiThread {
            controller.duration = videoModel.player.duration.toInt() / 10
            controller.buffedPosition = videoModel.player.bufferedPosition.toInt() / 10
            controller.updateProgress(videoModel.player.currentPosition)
        }
    }

    private fun updatePauseResume() {
        linePresenter.activityRef.get()?.runOnUiThread {
            controller.updatePauseResume(videoModel.player.playWhenReady)
        }
    }

    private fun init() {
        if (!inited) {
            inited = true
            view.visibility = View.VISIBLE

            var pauseOnStop = false
            linePresenter.proxy.onResumeListener = {
                if (videoModel.player.duration > 0 && pauseOnStop)
                    doPlayPause(true)
                pauseOnStop = false
            }
            linePresenter.proxy.onPauseListener = {
                pauseOnStop = videoModel.player.playWhenReady
                doPlayPause(false)
            }
            linePresenter.proxy.onStopListener = {
                pauseOnStop = videoModel.player.playWhenReady
                doPlayPause(false)
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
                linePresenter.proxy.item_mask.visibility =
                    if (state == BottomSheetBehavior.STATE_HIDDEN) View.INVISIBLE else View.VISIBLE
            }
            linePresenter.proxy.item_plugin.setOnApplyWindowInsetsListener { _, insets ->
                view.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
                insets.consumeSystemWindowInsets()
            }
            linePresenter.proxy.item_plugin.requestApplyInsets()
            view.lrc_view.setDraggable(true) {
                videoModel.player.seekTo(it)
                true
            }
        }
    }

    private fun play(
        providerEpisode: Provider.ProviderEpisode,
        info: LineInfo?
    ) {
        videoModel.player.playWhenReady = false
        linePresenter.proxy.subjectPresenter.subjectView.collapsibleAppBarHelper
            .setTitle(null, providerEpisode.title, null)
        playLoopTask?.cancel()
        controller.updatePrevNext(prevEpisode() != null, nextEpisode() != null)
        view.lrc_view.loadLrc("", "")
        controller.updateProgress(0)
        controller.updateLoading(true)
        linePresenter.subscribe(key = "play") {
            val provider = (LineProvider.getProvider(
                Provider.TYPE_MUSIC, info?.site ?: ""
            )?.provider as? MusicProvider) ?: return@subscribe
            val music = provider.getMusic("music", providerEpisode)
            videoModel.play(music)
            val lyric = provider.getLyric("lyric", providerEpisode)
            view.lrc_view.loadLrc(lyric.lrc, lyric.tlyric)
        }
    }

    override fun loadEp(episode: Episode) {
        val provider = episode.provider ?: return
        init()
        LineInfoModel.getInfo(linePresenter.subject).getDefaultProvider()?.let {
            prevEpisode = {
                val curIndex = linePresenter.episodeAdapter.data.indexOfFirst { it.provider == provider }
                linePresenter.episodeAdapter.data.getOrNull(curIndex - 1)
            }
            nextEpisode = {
                val curIndex = linePresenter.episodeAdapter.data.indexOfFirst { it.provider == provider }
                linePresenter.episodeAdapter.data.getOrNull(curIndex + 1)
            }
            startAt = null
            linePresenter.activityRef.get()?.runOnUiThread { play(provider, it) }
        }
    }

    override fun downloadEp(episode: Episode, updateInfo: (String) -> Unit) {
        TODO("Not yet implemented")
    }
}