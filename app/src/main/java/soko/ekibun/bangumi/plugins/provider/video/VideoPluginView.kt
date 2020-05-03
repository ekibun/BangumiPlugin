package soko.ekibun.bangumi.plugins.provider.video

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.StreamKey
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.danmaku_setting.view.*
import kotlinx.android.synthetic.main.error_frame.view.*
import kotlinx.android.synthetic.main.plugin_video.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.EpisodeCache
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.VideoModel
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.service.DownloadService
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.ui.view.VideoController
import soko.ekibun.bangumi.plugins.util.HttpUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil
import soko.ekibun.bangumi.plugins.util.NetworkUtil
import java.io.IOException
import java.util.*

class VideoPluginView(val linePresenter: LinePresenter) : Provider.PluginView(linePresenter, R.layout.plugin_video) {
    val isLandscape get() = linePresenter.activityRef.get()?.resources?.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isInMultiWindowMode get() = Build.VERSION.SDK_INT > 24 && linePresenter.activityRef.get()?.isInMultiWindowMode == true

    private fun updateView() {
        linePresenter.proxy.subjectPresenter.subjectView.behavior.isHideable = isLandscape
        view.player_container.layoutParams = (view.player_container.layoutParams as ConstraintLayout.LayoutParams).let {
            it.dimensionRatio = if (isLandscape) "" else "h,16:9"
            it
        }
        linePresenter.proxy.item_plugin.requestApplyInsets()
        view.post {
            resizeVideoSurface()
            danmakuPresenter.sizeScale = when {
                Build.VERSION.SDK_INT >= 24 && linePresenter.activityRef.get()?.isInPictureInPictureMode == true -> 0.7f
                isLandscape && !isInMultiWindowMode -> 1.1f
                else -> 0.8f
            }
            showInfo(false)
        }
        showInfo(false)
        controller.doShowHide(false)
    }

    val danmakuPresenter: DanmakuPresenter by lazy {
        DanmakuPresenter(linePresenter) {
            exception = it ?: exception
            loadDanmaku = it == null
        }
    }

    fun showInfo(show: Boolean) {
        linePresenter.proxy.subjectPresenter.subjectView.behavior.state =
            if (!show && isLandscape) BottomSheetBehavior.STATE_HIDDEN else BottomSheetBehavior.STATE_COLLAPSED
        linePresenter.proxy.item_mask.visibility = if (show) View.VISIBLE else View.INVISIBLE
        controller.doShowHide(false)
        updateActionBar()
    }

    val controller: VideoController by lazy {
        VideoController(view.controller_frame, object : VideoController.OnActionListener {
            override fun onPlayPause() {
                doPlayPause(!videoModel.player.playWhenReady)
            }

            override fun onFullscreen() {
                if (isLandscape) showInfo(true)
                linePresenter.activityRef.get()?.requestedOrientation = if (isLandscape)
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }

            override fun onNext() {
                nextEpisode()?.let { loadEp(it) }
            }

            override fun onPrev() {
                prevEpisode()?.let { loadEp(it) }
            }

            override fun onDanmaku() {
                if (view.danmaku_flame.isShown)
                    view.danmaku_flame.hide() else view.danmaku_flame.show()
                controller.updateDanmaku(view.danmaku_flame.isShown)
            }

            override fun seekTo(pos: Long) {
                videoModel.player.seekTo(pos)
                controller.updateProgress(videoModel.player.currentPosition)
            }

            override fun onDanmakuSetting() {
                showDanmakuSetting(true)
                controller.doShowHide(false)
            }

            override fun onTitle() {
                doPlayPause(false)
                showInfo(true)
//                context.app_bar.setExpanded(false)
            }

            override fun onShowHide(show: Boolean) {
                linePresenter.activityRef.get()?.runOnUiThread {
                    if (show) {
                        updatePauseResume()
                        updateProgress()
                        controller.updatePrevNext(prevEpisode() != null, nextEpisode() != null)
                        view.item_mask.visibility = View.VISIBLE
                    } else {
                        view.item_mask.visibility = View.INVISIBLE
                    }
                    updateActionBar()
                }
            }
        }) { isLandscape && !isInMultiWindowMode }
    }

    fun updateActionBar() {
        val maskShown =
            view.item_mask.visibility == View.VISIBLE || linePresenter.proxy.item_mask.visibility == View.VISIBLE
        linePresenter.proxy.app_bar.visibility = if (maskShown) View.VISIBLE else View.INVISIBLE
        linePresenter.activityRef.get()?.window?.decorView?.systemUiVisibility =
            if (!isLandscape || maskShown) View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    (if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION else 0)
            else (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

    }

    var loadVideoInfo: Boolean? = null
        set(v) {
            field = v
            parseLogcat()
        }
    var loadVideo: Boolean? = null
        set(v) {
            field = v
            parseLogcat()
        }
    var loadDanmaku: Boolean? = null
        set(v) {
            field = v
            parseLogcat()
        }
    var exception: Throwable? = null
        set(v) {
            field = v
            parseLogcat()
        }

    @SuppressLint("SetTextI18n")
    private fun parseLogcat() {
        linePresenter.activityRef.get()?.runOnUiThread {
            if (loadVideoInfo == false || loadVideo == false || exception != null)
                controller.updateLoading(false)
            view.item_logcat.text = "获取视频信息…" + if (loadVideoInfo == null) "" else (
                    if (loadVideoInfo != true) "【失败】" else ("【完成】" +
                            "\n解析视频地址…${if (loadVideo == null) "" else if (loadVideo == true) "【完成】" else "【失败】"}" +
                            "\n全舰弹幕装填…${if (loadDanmaku == null) "" else if (loadDanmaku == true) "【完成】" else "【失败】"}" +
                            if (loadVideo == true) "\n开始视频缓冲…" else "")) +
                    if (exception != null) "\n$exception" else ""
        }
    }

    var startAt: Long? = null
    var endFlag = false
    val videoModel: VideoModel by lazy {
        VideoModel(linePresenter, object : VideoModel.Listener {
            override fun onReady(playWhenReady: Boolean) {
                if (!controller.ctrVisibility) {
                    controller.ctrVisibility = true
                    view.item_logcat.visibility = View.INVISIBLE
                    controller.doShowHide(false)
                }
                if (playWhenReady) {
                    doPlayPause(true)
                    startAt?.let {
                        videoModel.player.seekTo(it)
                        startAt = null
                    }
                }
                if (!controller.isShow) view.item_mask.visibility = View.INVISIBLE
//                context.systemUIPresenter.updateSystemUI()
                controller.updateLoading(false)
                endFlag = true
            }

            override fun onBuffering() {
                view.danmaku_flame.pause()
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
                videoWidth = (width * pixelWidthHeightRatio).toInt()
                videoHeight = height
                resizeVideoSurface()
            }

            override fun onError(error: ExoPlaybackException) {
                showVideoError("视频加载错误\n${error.sourceException.localizedMessage}", "重新加载") {
                    startAt = videoModel.player.currentPosition
                    videoModel.reload()
                }
            }
        })
    }

    fun showVideoError(error: String, retry: String, callback: () -> Unit) {
        linePresenter.activityRef.get()?.runOnUiThread {
            //            context.videoPresenter.doPlayPause(false)
            controller.doShowHide(false)
            view.item_error_hint.text = error
            view.item_retry_button.text = retry
            view.error_frame.visibility = View.VISIBLE
            view.item_retry_button.setOnClickListener {
                view.error_frame.visibility = View.INVISIBLE
                callback()
            }
        }
    }

    private var playLoopTask: TimerTask? = null
    fun doPlayPause(play: Boolean) {
        videoModel.player.playWhenReady = play
        updatePauseResume()
        playLoopTask?.cancel()

        val doPlay = {
            playLoopTask = object : TimerTask() {
                override fun run() {
                    linePresenter.activityRef.get()?.runOnUiThread {
                        updateProgress()
//                    updatePlayProgress((videoModel.player.currentPosition/ 10).toInt())
                        danmakuPresenter.add(videoModel.player.currentPosition)
                        if (view.danmaku_flame.isShown && !view.danmaku_flame.isPaused) {
                            view.danmaku_flame.start(videoModel.player.currentPosition)
                        }
                    }
                }
            }
            controller.timer.schedule(playLoopTask, 0, 1000)
            view.video_surface.keepScreenOn = true
            if (videoModel.player.playbackState == Player.STATE_READY)
                view.danmaku_flame.resume()
        }
        if (play) {
            if (NetworkUtil.isWifiConnected(App.app.host) || ignoreNetwork) doPlay()
            else showVideoError("正在使用非wifi网络", "继续播放") {
                videoModel.player.playWhenReady = true
                updatePauseResume()
                ignoreNetwork = true
                doPlay()
            }
        } else {
            view.video_surface.keepScreenOn = false
            view.danmaku_flame.pause()
        }
    }

    private fun updateProgress() {
        linePresenter.activityRef.get()?.runOnUiThread {
            controller.duration = videoModel.player.duration.toInt() / 10
            controller.buffedPosition = videoModel.player.bufferedPosition.toInt() / 10
            controller.updateProgress(videoModel.player.currentPosition)
        }
    }

    private fun updatePauseResume() {
        linePresenter.activityRef.get()?.runOnUiThread {
            controller.updatePauseResume(videoModel.player.playWhenReady)
            setPictureInPictureParams(!videoModel.player.playWhenReady)
        }
    }

    var videoWidth = 0
    var videoHeight = 0
    fun resizeVideoSurface() {
        if (videoWidth * videoHeight == 0) return
        when (PreferenceManager.getDefaultSharedPreferences(linePresenter.pluginContext).getInt(
            DanmakuPresenter.VIDEO_FRAME,
            DanmakuPresenter.VIDEO_FRAME_AUTO
        )) {
            DanmakuPresenter.VIDEO_FRAME_AUTO -> {
                view.video_surface.scaleX = Math.min(
                    view.video_surface.measuredWidth.toFloat(),
                    (view.video_surface.measuredHeight * videoWidth / videoHeight).toFloat()
                ) / view.video_surface.measuredWidth
                view.video_surface.scaleY = Math.min(
                    view.video_surface.measuredHeight.toFloat(),
                    (view.video_surface.measuredWidth * videoHeight / videoWidth).toFloat()
                ) / view.video_surface.measuredHeight
            }
            DanmakuPresenter.VIDEO_FRAME_STRENTCH -> {
                view.video_surface.scaleX = 1f
                view.video_surface.scaleY = 1f
            }
            DanmakuPresenter.VIDEO_FRAME_FILL -> {
                view.video_surface.scaleX = Math.max(
                    view.video_surface.measuredWidth.toFloat(),
                    (view.video_surface.measuredHeight * videoWidth / videoHeight).toFloat()
                ) / view.video_surface.measuredWidth
                view.video_surface.scaleY = Math.max(
                    view.video_surface.measuredHeight.toFloat(),
                    (view.video_surface.measuredWidth * videoHeight / videoWidth).toFloat()
                ) / view.video_surface.measuredHeight
            }
            DanmakuPresenter.VIDEO_FRAME_16_9 -> {
                view.video_surface.scaleX = Math.min(
                    view.video_surface.measuredWidth.toFloat(),
                    (view.video_surface.measuredHeight * 16 / 9).toFloat()
                ) / view.video_surface.measuredWidth
                view.video_surface.scaleY = Math.min(
                    view.video_surface.measuredHeight.toFloat(),
                    (view.video_surface.measuredWidth * 9 / 16).toFloat()
                ) / view.video_surface.measuredHeight
            }
            DanmakuPresenter.VIDEO_FRAME_4_3 -> {
                view.video_surface.scaleX = Math.min(
                    view.video_surface.measuredWidth.toFloat(),
                    (view.video_surface.measuredHeight * 4 / 3).toFloat()
                ) / view.video_surface.measuredWidth
                view.video_surface.scaleY = Math.min(
                    view.video_surface.measuredHeight.toFloat(),
                    (view.video_surface.measuredWidth * 3 / 4).toFloat()
                ) / view.video_surface.measuredHeight
            }
        }
    }

    fun showDanmakuSetting(show: Boolean) {
        if (view.danmaku_setting_panel.visibility == if (show) View.VISIBLE else View.INVISIBLE) return
        view.danmaku_setting_panel.visibility = if (show) View.VISIBLE else View.INVISIBLE
        view.danmaku_setting_panel.animation = AnimationUtils.loadAnimation(
            linePresenter.pluginContext,
            if (show) R.anim.move_in_right else R.anim.move_out_right
        )
    }

    var nextEpisode: () -> Episode? = { null }
    var prevEpisode: () -> Episode? = { null }
    override fun loadEp(episode: Episode) {
        initPlayer()
        val infos = LineInfoModel.getInfo(linePresenter.subject)
        infos.getDefaultProvider()?.let {
            prevEpisode = {
                val position =
                    linePresenter.subjectView.episodeDetailAdapter.data.indexOfFirst {
                        Episode.compareEpisode(
                            it.t,
                            episode
                        )
                    }
                val ep = linePresenter.subjectView.episodeDetailAdapter.data.getOrNull(position - 1)?.t
                if ((ep?.status ?: "") !in listOf("Air")) null else ep
            }
            nextEpisode = {
                val position =
                    linePresenter.subjectView.episodeDetailAdapter.data.indexOfFirst {
                        Episode.compareEpisode(
                            it.t,
                            episode
                        )
                    }
                val ep = linePresenter.subjectView.episodeDetailAdapter.data.getOrNull(position + 1)?.t
                if ((ep?.status ?: "") !in listOf("Air")) null else ep
            }
            startAt = null
            linePresenter.activityRef.get()?.runOnUiThread { play(episode, it, infos.providers) }
        } ?: Toast.makeText(linePresenter.pluginContext, "请先添加播放源", Toast.LENGTH_SHORT).show()
    }

    var ignoreNetwork = false
    private fun play(episode: Episode, info: LineInfo, infos: List<LineInfo>) {
        ignoreNetwork = false
        videoModel.player.playWhenReady = false
//        context.systemUIPresenter.appbarCollapsible(false)
        loadVideoInfo = null
        loadVideo = null
        loadDanmaku = null
        exception = null
        showDanmakuSetting(false)
        linePresenter.activityRef.get()?.runOnUiThread {
            view.error_frame.visibility = View.INVISIBLE
//            view.toolbar_layout.isTitleEnabled = false
//            view.video_surface_container.visibility = View.VISIBLE
//            view.video_surface.visibility = View.VISIBLE
//            view.controller_frame.visibility = View.VISIBLE
            view.item_logcat.visibility = View.VISIBLE
//            view.toolbar.subtitle = episode.parseSort() + " - " + episode.name
        }
        controller.updatePrevNext(prevEpisode() != null, nextEpisode() != null)
        controller.updateLoading(true)
        controller.ctrVisibility = false
        controller.doShowHide(true)
        controller.setTitle(episode.parseSort(linePresenter.pluginContext) + " - " + episode.name)
        playLoopTask?.cancel()
        view.danmaku_flame.pause()
        view.item_logcat.setOnClickListener {}

        VideoModel.getVideo("play", linePresenter.subject, episode, info, Observable.create { emitter ->
            controller.updateLoading(false)
            view.item_logcat.visibility = View.INVISIBLE
            controller.doShowHide(false)
            showVideoError("正在使用非wifi网络", "继续加载") {
                ignoreNetwork = true
                controller.updateLoading(true)
                controller.doShowHide(true)
                view.item_logcat.visibility = View.VISIBLE
                emitter.onNext(true)
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe({
            if (linePresenter.activityRef.get()?.isDestroyed != false) return@subscribe
            Log.v("video", "Sub: $it")
            when (it) {
                is VideoProvider.VideoInfo -> {
                    val videoInfo = it
                    loadVideoInfo = true
                    view.item_logcat.setOnClickListener {
                        try {
                            linePresenter.activityRef.get()?.startActivity(
                                Intent.createChooser(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(videoInfo.url)
                                    ), videoInfo.url
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    danmakuPresenter.loadDanmaku(infos, episode)
                }
                is Pair<*, *> -> {
                    val (request: HttpUtil.HttpRequest?, streamKeys: List<StreamKey>?) = it as Pair<HttpUtil.HttpRequest?, List<StreamKey>?>
                    loadVideo = request != null
                    ignoreNetwork = ignoreNetwork || streamKeys != null
                    if (request != null) videoModel.play(request, view.video_surface, streamKeys)
                }
            }
        }, {
            if (loadVideoInfo != true) loadVideoInfo = false
            exception = it
        })
    }

    private fun setPictureInPictureParams(playPause: Boolean) {
        if (Build.VERSION.SDK_INT >= 26) {
            val actionPrev = RemoteAction(
                Icon.createWithResource(linePresenter.pluginContext, R.drawable.ic_prev), "上一集", "上一集",
                PendingIntent.getBroadcast(
                    App.app.host,
                    CONTROL_TYPE_PREV,
                    Intent(ACTION_MEDIA_CONTROL + linePresenter.subject.id).putExtra(
                        EXTRA_CONTROL_TYPE,
                        CONTROL_TYPE_PREV
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            actionPrev.isEnabled = prevEpisode() != null
            val actionNext = RemoteAction(
                Icon.createWithResource(linePresenter.pluginContext, R.drawable.ic_next), "下一集", "下一集",
                PendingIntent.getBroadcast(
                    App.app.host,
                    CONTROL_TYPE_NEXT,
                    Intent(ACTION_MEDIA_CONTROL + linePresenter.subject.id).putExtra(
                        EXTRA_CONTROL_TYPE,
                        CONTROL_TYPE_NEXT
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            actionNext.isEnabled = nextEpisode() != null
            try {
                linePresenter.activityRef.get()?.setPictureInPictureParams(
                    PictureInPictureParams.Builder().setActions(
                        listOf(
                            actionPrev,
                            RemoteAction(
                                Icon.createWithResource(
                                    linePresenter.pluginContext,
                                    if (playPause) R.drawable.ic_play else R.drawable.ic_pause
                                ), if (playPause) "播放" else "暂停", if (playPause) "播放" else "暂停",
                                PendingIntent.getBroadcast(
                                    App.app.host,
                                    CONTROL_TYPE_PLAY,
                                    Intent(ACTION_MEDIA_CONTROL + linePresenter.subject.id).putExtra(
                                        EXTRA_CONTROL_TYPE,
                                        if (playPause) CONTROL_TYPE_PLAY else CONTROL_TYPE_PAUSE
                                    ),
                                    PendingIntent.FLAG_UPDATE_CURRENT
                                )
                            ),
                            actionNext
                        )
                    ).build()
                )
            } catch (e: Exception) {
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                CONTROL_TYPE_PAUSE -> {
                    doPlayPause(false)
                }
                CONTROL_TYPE_PLAY -> {
                    doPlayPause(true)
                }
                CONTROL_TYPE_NEXT -> {
                    nextEpisode()?.let { loadEp(it) }
                }
                CONTROL_TYPE_PREV -> {
                    prevEpisode()?.let { loadEp(it) }
                }
            }
        }
    }

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!NetworkUtil.isWifiConnected(context) && videoModel.player.playWhenReady) {
                doPlayPause(true)
                Toast.makeText(context, "正在使用非wifi网络", Toast.LENGTH_LONG).show()
            }
        }
    }

    init {
        linePresenter.activityRef.get()
            ?.registerReceiver(receiver, IntentFilter(ACTION_MEDIA_CONTROL + linePresenter.subject.id))
        linePresenter.activityRef.get()
            ?.registerReceiver(networkReceiver, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
        linePresenter.onDestroyListener = {
            linePresenter.activityRef.get()?.unregisterReceiver(receiver)
            linePresenter.activityRef.get()?.unregisterReceiver(networkReceiver)
        }
    }

    private var inited = false
    private fun initPlayer() {
        if (inited) return
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
            if (linePresenter.activityRef.get()?.isInPictureInPictureMode != true)
                doPlayPause(false)
        }
        linePresenter.proxy.onStopListener = {
            pauseOnStop = videoModel.player.playWhenReady
            doPlayPause(false)
        }

        linePresenter.proxy.subjectPresenter.subjectView.onStateChangedListener = onStateChangedListener@{ state ->
            if (!isLandscape) return@onStateChangedListener
            if (state == BottomSheetBehavior.STATE_COLLAPSED) doPlayPause(false)
            val maskVisibility = if (state == BottomSheetBehavior.STATE_HIDDEN) View.INVISIBLE else View.VISIBLE
            linePresenter.proxy.item_mask.visibility = maskVisibility
            linePresenter.proxy.app_bar.visibility = maskVisibility
        }

        linePresenter.proxy.subjectPresenter.subjectView.collapsibleAppBarHelper.let {
            val superTitleClick = it.onTitleClickListener
            it.onTitleClickListener = { ev ->
                if (linePresenter.proxy.subjectPresenter.subjectView.behavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    doPlayPause(false)
                    showInfo(true)
                } else superTitleClick(ev)
            }
        }

        linePresenter.proxy.onBackListener = {
            val behavior = linePresenter.proxy.subjectPresenter.subjectView.behavior
            if (isInMultiWindowMode && isLandscape && behavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                showInfo(false)
                true
            } else if (isInMultiWindowMode) {
                Toast.makeText(linePresenter.pluginContext, "请先退出多窗口模式", Toast.LENGTH_LONG).show()
                true
            } else if (isLandscape && behavior.state == BottomSheetBehavior.STATE_HIDDEN && !isInMultiWindowMode) {
                showInfo(true)
                linePresenter.activityRef.get()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                true
            } else false
        }
        linePresenter.proxy.onUserLeaveHintListener = {
            if (isLandscape && !isInMultiWindowMode && videoModel.player.playWhenReady && Build.VERSION.SDK_INT >= 24) {
                @Suppress("DEPRECATION") linePresenter.activityRef.get()?.enterPictureInPictureMode()
                setPictureInPictureParams(false)
            }
        }
        linePresenter.proxy.item_plugin.setOnApplyWindowInsetsListener { _, insets ->
            view.setPadding(
                0,
                if (isLandscape && !isInMultiWindowMode) 0 else insets.systemWindowInsetTop,
                0,
                if (isLandscape) 0 else insets.systemWindowInsetBottom
            )
            view.controller_frame.setPadding(
                0, 0, 0,
                if (!isLandscape) 0 else insets.systemWindowInsetBottom
            )
            insets.consumeSystemWindowInsets()
        }
        view.hide_danmaku_panel.setOnClickListener {
            showDanmakuSetting(false)
        }
        linePresenter.proxy.subjectPresenter.subjectView.peakMargin = 9 / 16f
        view.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
            if (l != ol || t != ot || r != or || b != ob) updateView()
        }
        linePresenter.proxy.item_mask.setOnClickListener {
            if (isLandscape) showInfo(false)
        }
        updateView()
    }

    override fun downloadEp(episode: Episode, updateInfo: (String) -> Unit) {
        val subject = linePresenter.proxy.subjectPresenter.subject
        val info = LineInfoModel.getInfo(subject).getDefaultProvider() ?: return
        updateInfo("获取视频信息")
        VideoModel.getVideo(episode.parseSort(App.app.plugin), subject, episode, info, Observable.just(true))
            .observeOn(AndroidSchedulers.mainThread()).subscribe({
                when (it) {
                    is VideoProvider.VideoInfo -> {
                        updateInfo("解析视频地址")
                    }
                    is Pair<*, *> -> {
                        val (request: HttpUtil.HttpRequest?, streamKeys: List<StreamKey>?) = it as Pair<HttpUtil.HttpRequest?, List<StreamKey>?>
                        if (request == null || request.url.startsWith("/")) return@subscribe
                        updateInfo("创建视频请求")
                        VideoModel.createDownloadRequest(request, object : DownloadHelper.Callback {
                            override fun onPrepared(helper: DownloadHelper) {
                                val downloadRequest = helper.getDownloadRequest(request.url, null)
                                DownloadService.download(
                                    App.app.plugin, episode, subject, EpisodeCache(
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
            }, {
                updateInfo("解析视频出错：${it?.message}")
            })
    }

    companion object {
        const val ACTION_MEDIA_CONTROL = "soko.ekibun.bangumi.plugin.video.mediaControl"
        const val EXTRA_CONTROL_TYPE = "extraControlType"
        const val CONTROL_TYPE_PAUSE = 1
        const val CONTROL_TYPE_PLAY = 2
        const val CONTROL_TYPE_NEXT = 3
        const val CONTROL_TYPE_PREV = 4
    }
}