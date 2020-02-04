package soko.ekibun.bangumi.plugins.provider.video

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.danmaku_setting.view.*
import kotlinx.android.synthetic.main.error_frame.view.*
import kotlinx.android.synthetic.main.plugin_video.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.VideoModel
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.ui.view.VideoController
import soko.ekibun.bangumi.plugins.util.NetworkUtil
import java.util.*

class VideoPluginView(linePresenter: LinePresenter) : Provider.PluginView(linePresenter, R.layout.plugin_video) {
    val isLandscape get() = linePresenter.context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    private fun updateView() {
        linePresenter.setHideable(isLandscape)
        view.player_container.layoutParams = (view.player_container.layoutParams as ConstraintLayout.LayoutParams).let {
            it.dimensionRatio = if (isLandscape) "" else "h,16:9"
            it
        }
        linePresenter.pluginContainer.requestApplyInsets()
        view.post {
            resizeVideoSurface()
        }
        linePresenter.maskView.visibility = View.INVISIBLE
        linePresenter.setState(if(isLandscape) BottomSheetBehavior.STATE_HIDDEN else BottomSheetBehavior.STATE_COLLAPSED)
        controller.doShowHide(false)
    }

    val danmakuPresenter: DanmakuPresenter by lazy{
        DanmakuPresenter(linePresenter) {
            exception = it?:exception
            loadDanmaku = it == null
        }
    }

    fun showInfo(show: Boolean){
        linePresenter.setState(if (!show && isLandscape) BottomSheetBehavior.STATE_HIDDEN else BottomSheetBehavior.STATE_COLLAPSED)
        linePresenter.maskView.visibility = if(show) View.VISIBLE else View.INVISIBLE
        linePresenter.appbar.visibility = linePresenter.maskView.visibility
    }

    val controller: VideoController by lazy {
        VideoController(view.controller_frame, object : VideoController.OnActionListener {
            override fun onPlayPause() {
                doPlayPause(!videoModel.player.playWhenReady)
            }

            override fun onFullscreen() {
                linePresenter.context.requestedOrientation = if(isLandscape)
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }

            override fun onNext() {
                nextEpisode()?.let{ loadEp(it) }
            }

            override fun onPrev() {
                prevEpisode()?.let{ loadEp(it) }
            }

            override fun onDanmaku() {
                if(view.danmaku_flame.isShown)
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
                linePresenter.context.runOnUiThread {
                    if (show) {
                        updatePauseResume()
                        updateProgress()
                        controller.updatePrevNext(prevEpisode() != null, nextEpisode() != null)
                        view.item_mask.visibility = View.VISIBLE
                    } else {
                        view.item_mask.visibility = View.INVISIBLE
                    }
//                    context.systemUIPresenter.updateSystemUI()
                }
            }
        }) { linePresenter.context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
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
    var exception: Exception? = null
        set(v) {
            field = v
            parseLogcat()
        }

    @SuppressLint("SetTextI18n")
    private fun parseLogcat() {
        linePresenter.context.runOnUiThread {
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
    val videoModel: VideoModel by lazy{
        VideoModel(linePresenter, object : VideoModel.Listener {
            override fun onReady(playWhenReady: Boolean) {
                if (!controller.ctrVisibility) {
                    controller.ctrVisibility = true
                    view.item_logcat.visibility = View.INVISIBLE
                    controller.doShowHide(false)
                }
                if (playWhenReady) {
                    doPlayPause(true)
                    startAt?.let{
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
                if(endFlag) {
                    endFlag = false
                    nextEpisode()?.let { loadEp(it) }
                }
            }

            override fun onVideoSizeChange(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                videoWidth = (width * pixelWidthHeightRatio).toInt()
                videoHeight = height
                resizeVideoSurface()
            }

            override fun onError(error: ExoPlaybackException) {
//                Log.e("plugin", Log.getStackTraceString(error.sourceException))
//                showVideoError("视频加载错误\n${error.sourceException.localizedMessage}", "重新加载"){
//                    startAt = videoModel.player.currentPosition
//                    videoModel.reload()
//                }
            }
        })
    }

    fun showVideoError(error: String, retry: String, callback: () -> Unit) {
        linePresenter.context.runOnUiThread {
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
    fun doPlayPause(play: Boolean){
        videoModel.player.playWhenReady = play
        updatePauseResume()
        playLoopTask?.cancel()

        val doPlay = {
            playLoopTask = object: TimerTask(){ override fun run() {
                linePresenter.context.runOnUiThread {
                    updateProgress()
//                    updatePlayProgress((videoModel.player.currentPosition/ 10).toInt())
                    danmakuPresenter.add(videoModel.player.currentPosition)
                    if(view.danmaku_flame.isShown && !view.danmaku_flame.isPaused){
                        view.danmaku_flame.start(videoModel.player.currentPosition)
                    }
                }
            } }
            controller.timer.schedule(playLoopTask, 0, 1000)
            view.video_surface.keepScreenOn = true
            if(videoModel.player.playbackState == Player.STATE_READY)
                view.danmaku_flame.resume()
        }
        if(play){
            if(NetworkUtil.isWifiConnected(linePresenter.context) || ignoreNetwork) doPlay()
            else showVideoError("正在使用非wifi网络", "继续播放"){
                videoModel.player.playWhenReady = true
                updatePauseResume()
                ignoreNetwork = true
                doPlay()
            }
        }else{
            view.video_surface.keepScreenOn = false
            view.danmaku_flame.pause()
        }
    }

    private fun updateProgress(){
        linePresenter.context.runOnUiThread {
            controller.duration = videoModel.player.duration.toInt() /10
            controller.buffedPosition = videoModel.player.bufferedPosition.toInt() /10
            controller.updateProgress(videoModel.player.currentPosition)
        }
    }

    private fun updatePauseResume() {
        linePresenter.context.runOnUiThread {
            controller.updatePauseResume(videoModel.player.playWhenReady)
//            context.setPictureInPictureParams(!videoModel.player.playWhenReady)
        }
    }

    var videoWidth = 0
    var videoHeight = 0
    fun resizeVideoSurface(){
        if(videoWidth * videoHeight == 0) return
        when(PreferenceManager.getDefaultSharedPreferences(linePresenter.pluginContext).getInt(DanmakuPresenter.VIDEO_FRAME, DanmakuPresenter.VIDEO_FRAME_AUTO)){
            DanmakuPresenter.VIDEO_FRAME_AUTO -> {
                view.video_surface.scaleX = Math.min(view.video_surface.measuredWidth.toFloat(), (view.video_surface.measuredHeight * videoWidth / videoHeight).toFloat()) / view.video_surface.measuredWidth
                view.video_surface.scaleY = Math.min(view.video_surface.measuredHeight.toFloat(), (view.video_surface.measuredWidth * videoHeight / videoWidth).toFloat()) / view.video_surface.measuredHeight
            }
            DanmakuPresenter.VIDEO_FRAME_STRENTCH -> {
                view.video_surface.scaleX = 1f
                view.video_surface.scaleY = 1f
            }
            DanmakuPresenter.VIDEO_FRAME_FILL -> {
                view.video_surface.scaleX = Math.max(view.video_surface.measuredWidth.toFloat(), (view.video_surface.measuredHeight * videoWidth / videoHeight).toFloat()) / view.video_surface.measuredWidth
                view.video_surface.scaleY = Math.max(view.video_surface.measuredHeight.toFloat(), (view.video_surface.measuredWidth * videoHeight / videoWidth).toFloat()) / view.video_surface.measuredHeight
            }
            DanmakuPresenter.VIDEO_FRAME_16_9 -> {
                view.video_surface.scaleX = Math.min(view.video_surface.measuredWidth.toFloat(), (view.video_surface.measuredHeight * 16 / 9).toFloat()) / view.video_surface.measuredWidth
                view.video_surface.scaleY = Math.min(view.video_surface.measuredHeight.toFloat(), (view.video_surface.measuredWidth * 9 / 16).toFloat()) / view.video_surface.measuredHeight
            }
            DanmakuPresenter.VIDEO_FRAME_4_3 -> {
                view.video_surface.scaleX = Math.min(view.video_surface.measuredWidth.toFloat(), (view.video_surface.measuredHeight * 4 / 3).toFloat()) / view.video_surface.measuredWidth
                view.video_surface.scaleY = Math.min(view.video_surface.measuredHeight.toFloat(), (view.video_surface.measuredWidth * 3 / 4).toFloat()) / view.video_surface.measuredHeight
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

    var nextEpisode: ()->Episode? = { null }
    var prevEpisode: ()->Episode? = { null }
    override fun loadEp(ep: Episode){
        val infos = linePresenter.app.lineInfoModel.getInfos(linePresenter.subject())
        infos?.getDefaultProvider()?.let{
            prevEpisode = {
                val position = linePresenter.mySubjectView.episodeDetailAdapter.data.indexOfFirst { it.t?.id == ep.id || (it.t?.type == ep.type && it.t?.sort == ep.sort) }
                val episode = linePresenter.mySubjectView.episodeDetailAdapter.data.getOrNull(position-1)?.t
                if((episode?.status?:"") !in listOf("Air")) null else episode
            }
            nextEpisode = {
                val position = linePresenter.mySubjectView.episodeDetailAdapter.data.indexOfFirst { it.t?.id == ep.id || (it.t?.type == ep.type && it.t?.sort == ep.sort) }
                val episode = linePresenter.mySubjectView.episodeDetailAdapter.data.getOrNull(position+1)?.t
                if((episode?.status?:"") !in listOf("Air")) null else episode
            }
            startAt = null
            linePresenter.context.runOnUiThread { play(ep, it, infos.providers) }
        }?: Toast.makeText(linePresenter.context, "请先添加播放源", Toast.LENGTH_SHORT).show()
    }

    var ignoreNetwork = false
    private fun play(episode: Episode, info: LineInfoModel.LineInfo, infos: List<LineInfoModel.LineInfo>) {
        ignoreNetwork = false
        videoModel.player.playWhenReady = false
//        context.systemUIPresenter.appbarCollapsible(false)
        loadVideoInfo = null
        loadVideo = null
        loadDanmaku = null
        exception = null
        showDanmakuSetting(false)
        linePresenter.context.runOnUiThread {
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

        videoModel.getVideo("play", episode, info, {videoInfo, error->
            exception = error?:exception
            loadVideoInfo = videoInfo != null
            if(videoInfo != null) linePresenter.context.runOnUiThread {
                view.item_logcat.setOnClickListener {
                    try{ linePresenter.context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(videoInfo.url)), videoInfo.url)) }
                    catch (e: Exception){ e.printStackTrace() }}
                danmakuPresenter.loadDanmaku(infos, episode) }
        },{request, streamKeys, error ->
            exception = error?:exception
            if(linePresenter.context.isDestroyed) return@getVideo
            loadVideo = request != null
            ignoreNetwork = streamKeys != null
            if(request != null) videoModel.play(request, view.video_surface, streamKeys)
        }, {
            controller.updateLoading(false)
            view.item_logcat.visibility = View.INVISIBLE
            controller.doShowHide(false)
            showVideoError("正在使用非wifi网络", "继续加载"){
                ignoreNetwork = true
                controller.updateLoading(true)
                controller.doShowHide(true)
                view.item_logcat.visibility = View.VISIBLE
                it()
            }
        })
    }

    init {
        linePresenter.appbar.visibility = View.INVISIBLE
        linePresenter.pluginContainer.setOnApplyWindowInsetsListener { _, insets ->
            view.setPadding(
                0,
                if (isLandscape) 0 else insets.systemWindowInsetTop,
                0,
                if (isLandscape) 0 else insets.systemWindowInsetBottom
            )
            insets.consumeSystemWindowInsets()
        }
        view.hide_danmaku_panel.setOnClickListener {
            showDanmakuSetting(false)
        }
        linePresenter.setPeakMargin(9 / 16f)
        view.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
            if(l != ol || t != ot || r != or || b != ob) updateView()
        }
        linePresenter.maskView.setOnClickListener {
            if(isLandscape) showInfo(false)
        }
        updateView()
    }
}