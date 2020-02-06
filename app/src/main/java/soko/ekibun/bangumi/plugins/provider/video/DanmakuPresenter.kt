package soko.ekibun.bangumi.plugins.provider.video

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.widget.SeekBar
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.danmaku_setting.view.*
import kotlinx.android.synthetic.main.plugin_video.view.*
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import soko.ekibun.bangumi.plugins.JsEngine
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class DanmakuPresenter(
    val linePresenter: LinePresenter,
    private val onFinish: (Exception?) -> Unit
) {
    private val sp by lazy { PreferenceManager.getDefaultSharedPreferences(linePresenter.pluginContext) }
    private val danmakuContext by lazy { DanmakuContext.create() }
    private val parser by lazy {
        object : BaseDanmakuParser() {
            override fun parse(): Danmakus {
                return Danmakus()
            }
        }
    }
    var sizeScale = 0.8f
        set(value) {
            field = value
            updateValue()
        }
    private val adapter = DanmakuListAdapter()

    init {
        val overlappingEnablePair = HashMap<Int, Boolean>()
        overlappingEnablePair[BaseDanmaku.TYPE_SCROLL_LR] = true
        overlappingEnablePair[BaseDanmaku.TYPE_FIX_BOTTOM] = true
        BaseDanmaku.TYPE_MOVEABLE_XXX
        danmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3f)
            .setDuplicateMergingEnabled(true)
            .preventOverlapping(overlappingEnablePair)
        linePresenter.pluginView.view.danmaku_flame.prepare(parser, danmakuContext)
        linePresenter.pluginView.view.danmaku_flame.enableDanmakuDrawingCache(true)

        updateValue()
        val seekBarChange = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                when (seekBar) {
                    linePresenter.pluginView.view.danmaku_opac_seek -> sp.edit().putInt(DANMAKU_OPACITY, progress).apply()
                    linePresenter.pluginView.view.danmaku_size_seek -> sp.edit().putInt(DANMAKU_SIZE, progress + 50).apply()
                    linePresenter.pluginView.view.danmaku_loc_seek -> sp.edit().putInt(DANMAKU_LOCATION, progress).apply()
                    linePresenter.pluginView.view.danmaku_speed_seek -> sp.edit().putInt(DANMAKU_SPEED, progress).apply()
                }
                updateValue()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        linePresenter.pluginView.view.danmaku_opac_seek.setOnSeekBarChangeListener(seekBarChange)
        linePresenter.pluginView.view.danmaku_size_seek.setOnSeekBarChangeListener(seekBarChange)
        linePresenter.pluginView.view.danmaku_loc_seek.setOnSeekBarChangeListener(seekBarChange)
        linePresenter.pluginView.view.danmaku_speed_seek.setOnSeekBarChangeListener(seekBarChange)

        val onClick = View.OnClickListener { view: View ->
            val key = when (view) {
                view.danmaku_top -> DANMAKU_ENABLE_TOP
                view.danmaku_scroll -> DANMAKU_ENABLE_SCROLL
                view.danmaku_bottom -> DANMAKU_ENABLE_BOTTOM
                view.danmaku_special -> DANMAKU_ENABLE_SPECIAL
                else -> return@OnClickListener
            }
            sp.edit().putBoolean(key, !sp.getBoolean(key, true)).apply()
            updateValue()
        }
        linePresenter.pluginView.view.danmaku_top.setOnClickListener(onClick)
        linePresenter.pluginView.view.danmaku_scroll.setOnClickListener(onClick)
        linePresenter.pluginView.view.danmaku_bottom.setOnClickListener(onClick)
        linePresenter.pluginView.view.danmaku_special.setOnClickListener(onClick)

        val onClickVideoFrame = View.OnClickListener { view: View ->
            val key = when (view) {
                view.video_frame_auto -> VIDEO_FRAME_AUTO
                view.video_frame_stretch -> VIDEO_FRAME_STRENTCH
                view.video_frame_fill -> VIDEO_FRAME_FILL
                view.video_frame_16_9 -> VIDEO_FRAME_16_9
                view.video_frame_4_3 -> VIDEO_FRAME_4_3
                else -> return@OnClickListener
            }
            sp.edit().putInt(VIDEO_FRAME, key).apply()
            (linePresenter.pluginView as VideoPluginView).resizeVideoSurface()
            updateValue()
        }
        linePresenter.pluginView.view.video_frame_auto.setOnClickListener(onClickVideoFrame)
        linePresenter.pluginView.view.video_frame_stretch.setOnClickListener(onClickVideoFrame)
        linePresenter.pluginView.view.video_frame_fill.setOnClickListener(onClickVideoFrame)
        linePresenter.pluginView.view.video_frame_16_9.setOnClickListener(onClickVideoFrame)
        linePresenter.pluginView.view.video_frame_4_3.setOnClickListener(onClickVideoFrame)

        linePresenter.pluginView.view.item_danmaku_list.isNestedScrollingEnabled = false
        linePresenter.pluginView.view.item_danmaku_list.layoutManager = LinearLayoutManager(linePresenter.pluginContext)
        linePresenter.pluginView.view.item_danmaku_list.adapter = adapter
    }

    @SuppressLint("SetTextI18n")
    fun updateValue() {
        val colorActive = ResourceUtil.resolveColorAttr(linePresenter.pluginContext, R.attr.colorPrimary)
        //videoFrame
        val videoFrame = sp.getInt(VIDEO_FRAME, VIDEO_FRAME_AUTO)
        linePresenter.pluginView.view.video_frame_auto.setTextColor(if (videoFrame == VIDEO_FRAME_AUTO) colorActive else Color.WHITE)
        linePresenter.pluginView.view.video_frame_stretch.setTextColor(if (videoFrame == VIDEO_FRAME_STRENTCH) colorActive else Color.WHITE)
        linePresenter.pluginView.view.video_frame_fill.setTextColor(if (videoFrame == VIDEO_FRAME_FILL) colorActive else Color.WHITE)
        linePresenter.pluginView.view.video_frame_16_9.setTextColor(if (videoFrame == VIDEO_FRAME_16_9) colorActive else Color.WHITE)
        linePresenter.pluginView.view.video_frame_4_3.setTextColor(if (videoFrame == VIDEO_FRAME_4_3) colorActive else Color.WHITE)
        //block
        danmakuContext.ftDanmakuVisibility = sp.getBoolean(DANMAKU_ENABLE_TOP, true)
        danmakuContext.r2LDanmakuVisibility = sp.getBoolean(DANMAKU_ENABLE_SCROLL, true)
        danmakuContext.l2RDanmakuVisibility = sp.getBoolean(DANMAKU_ENABLE_SCROLL, true)
        danmakuContext.fbDanmakuVisibility = sp.getBoolean(DANMAKU_ENABLE_BOTTOM, true)
        danmakuContext.SpecialDanmakuVisibility = sp.getBoolean(DANMAKU_ENABLE_SPECIAL, true)
        linePresenter.pluginView.view.danmaku_top.setTextColor(if (danmakuContext.ftDanmakuVisibility) colorActive else Color.WHITE)
        linePresenter.pluginView.view.danmaku_scroll.setTextColor(if (danmakuContext.r2LDanmakuVisibility) colorActive else Color.WHITE)
        linePresenter.pluginView.view.danmaku_bottom.setTextColor(if (danmakuContext.fbDanmakuVisibility) colorActive else Color.WHITE)
        linePresenter.pluginView.view.danmaku_special.setTextColor(if (danmakuContext.SpecialDanmakuVisibility) colorActive else Color.WHITE)
        //opacity
        linePresenter.pluginView.view.danmaku_opac_seek.progress = sp.getInt(DANMAKU_OPACITY, 100)
        linePresenter.pluginView.view.danmaku_opac_value.text = "${linePresenter.pluginView.view.danmaku_opac_seek.progress}%"
        danmakuContext.setDanmakuTransparency(linePresenter.pluginView.view.danmaku_opac_seek.progress / 100f)
        //size
        linePresenter.pluginView.view.danmaku_size_seek.progress = sp.getInt(DANMAKU_SIZE, 100) - 50
        linePresenter.pluginView.view.danmaku_size_value.text = "${linePresenter.pluginView.view.danmaku_size_seek.progress + 50}%"
        danmakuContext.setScaleTextSize(sizeScale * (linePresenter.pluginView.view.danmaku_size_seek.progress + 50) / 100f)
        //location
        val maxLinesPair = HashMap<Int, Int>()
        linePresenter.pluginView.view.danmaku_loc_seek.progress = sp.getInt(DANMAKU_LOCATION, 4)
        linePresenter.pluginView.view.danmaku_loc_value.text = when (linePresenter.pluginView.view.danmaku_loc_seek.progress) {
            0 -> "1/4屏"
            1 -> "半屏"
            2 -> "3/4屏"
            3 -> "满屏"
            else -> "无限"
        }
        maxLinesPair[BaseDanmaku.TYPE_SCROLL_RL] = Math.ceil(
            linePresenter.pluginView.view.player_container.height / (50 * sizeScale * (linePresenter.pluginView.view.danmaku_size_seek.progress + 50) / 100.0) * when (linePresenter.pluginView.view.danmaku_loc_seek.progress) {
                0 -> 0.25
                1 -> 0.5
                2 -> 0.75
                3 -> 1.0
                else -> 1000.0
            }
        ).toInt()
        danmakuContext.setMaximumLines(maxLinesPair)
        //speed
        linePresenter.pluginView.view.danmaku_speed_seek.progress = sp.getInt(DANMAKU_SPEED, 2)
        linePresenter.pluginView.view.danmaku_speed_value.text = when (linePresenter.pluginView.view.danmaku_speed_seek.progress) {
            0 -> "极慢"
            1 -> "较慢"
            2 -> "适中"
            3 -> "较快"
            else -> "极快"
        }
        danmakuContext.setScrollSpeedFactor(
            1.2f * when (linePresenter.pluginView.view.danmaku_speed_seek.progress) {
                0 -> 2f
                1 -> 1.5f
                2 -> 1f
                3 -> 0.75f
                else -> 0.5f
            }
        )
    }

    companion object {
        const val DANMAKU_OPACITY = "danmakuOpacity"
        const val DANMAKU_SIZE = "danmakuSize"
        const val DANMAKU_SPEED = "danmakuSpeed"
        const val DANMAKU_LOCATION = "danmakuLocation"

        const val DANMAKU_ENABLE_TOP = "danmakuEnableTop"
        const val DANMAKU_ENABLE_SCROLL = "danmakuEnableScroll"
        const val DANMAKU_ENABLE_BOTTOM = "danmakuEnableBottom"
        const val DANMAKU_ENABLE_SPECIAL = "danmakuEnableSpecial"

        const val VIDEO_FRAME = "videoFrame"
        const val VIDEO_FRAME_AUTO = 0
        const val VIDEO_FRAME_STRENTCH = 1
        const val VIDEO_FRAME_FILL = 2
        const val VIDEO_FRAME_16_9 = 3
        const val VIDEO_FRAME_4_3 = 4
    }

    private val videoInfoCalls = ArrayList<JsEngine.ScriptTask<VideoProvider.VideoInfo>>()
    private val danmakuCalls: ArrayList<JsEngine.ScriptTask<String>> = ArrayList()
    private val danmakuKeys: HashMap<DanmakuListAdapter.DanmakuInfo, String> = HashMap()
    fun loadDanmaku(lines: List<LineInfoModel.LineInfo>, episode: Episode) {
        linePresenter.pluginView.view.danmaku_flame.removeAllDanmakus(true)
        danmakuCalls.forEach { it.cancel(true) }
        danmakuCalls.clear()
        danmakuKeys.clear()

        videoInfoCalls.forEach { it.cancel(true) }
        videoInfoCalls.clear()

        adapter.setNewData(lines.map { DanmakuListAdapter.DanmakuInfo(it) })
        adapter.data.forEach {
            loadDanmaku(it, episode)
        }
        adapter.setOnItemClickListener { _, _, position ->
            loadDanmaku(adapter.data[position], episode)
        }
    }

    private fun loadDanmaku(danmakuInfo: DanmakuListAdapter.DanmakuInfo, episode: Episode) {
        val provider = LineProvider.getProvider(Provider.TYPE_VIDEO, danmakuInfo.line.site)?.provider as? VideoProvider ?: return
        when {
            danmakuInfo.videoInfo == null -> {
                danmakuInfo.info = " 获取视频信息..."
                linePresenter.activity.runOnUiThread { adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
                val videoCall = provider.getVideoInfo(
                    "getVideoInfo(${danmakuInfo.line}, ${episode.id})",
                    linePresenter.app.jsEngine,
                    danmakuInfo.line,
                    episode
                )
                videoInfoCalls.add(videoCall)
                videoCall.enqueue({ videoInfo ->
                    danmakuInfo.videoInfo = videoInfo
                    loadDanmaku(danmakuInfo, episode)
                }, {
                    danmakuInfo.info = " 获取视频信息出错: $it"
                    linePresenter.activity.runOnUiThread { adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
                    onFinish(it)
                })
            }
            danmakuInfo.key == null -> {
                danmakuInfo.info = " 获取弹幕信息..."
                linePresenter.activity.runOnUiThread { adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
                val call = provider.getDanmakuKey(
                    "getDanmakuKey(${danmakuInfo.videoInfo})",
                    linePresenter.app.jsEngine,
                    danmakuInfo.videoInfo ?: return
                )
                danmakuCalls.add(call)
                call.enqueue({
                    danmakuInfo.key = it
                    doAdd(Math.max(lastPos, 0) * 1000L * 300L, danmakuInfo)
                }, {
                    danmakuInfo.info = " 获取弹幕信息出错: $it"
                    linePresenter.activity.runOnUiThread { adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
                    onFinish(it)
                })
            }
            else -> doAdd(Math.max(lastPos, 0) * 1000L * 300L, danmakuInfo)
        }
    }

    private fun doAdd(pos: Long, danmakuInfo: DanmakuListAdapter.DanmakuInfo) {
        val provider = LineProvider.getProvider(Provider.TYPE_VIDEO, danmakuInfo.line.site)?.provider as? VideoProvider ?: return
        val call = provider.getDanmaku(
            "getDanmakuKey(${danmakuInfo.videoInfo}, ${danmakuInfo.key}, ${pos / 1000})",
            linePresenter.app.jsEngine,
            danmakuInfo.videoInfo ?: return,
            danmakuInfo.key ?: return,
            (pos / 1000).toInt()
        )
        danmakuInfo.info = " 加载弹幕..."
        linePresenter.activity.runOnUiThread { adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
        call.enqueue({
            it.forEach {
                danmakuInfo.danmakus.add(it)

                val danmaku = danmakuContext.mDanmakuFactory.createDanmaku(it.type, danmakuContext) ?: return@forEach
                danmaku.time = (it.time * 1000).toLong()
                danmaku.textSize = it.textSize * (parser.displayer.density - 0.6f)
                danmaku.textColor = it.color
                danmaku.textShadowColor = if (it.color <= Color.BLACK) Color.WHITE else Color.BLACK
                danmaku.text = it.content
                linePresenter.pluginView.view.danmaku_flame.addDanmaku(danmaku)
            }
            danmakuInfo.info = ""
            linePresenter.activity.runOnUiThread { adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
            onFinish(null)
        }, {
            danmakuInfo.info = " 加载弹幕出错: $it"
            linePresenter.activity.runOnUiThread { adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
            onFinish(it)
        })
    }

    private var lastPos = -1
    fun add(pos: Long) {
        val newPos = (pos / 1000).toInt() / 300
        if (lastPos == -1 || lastPos != newPos) {
            lastPos = newPos
            adapter.data.forEach { doAdd(pos, it) }
        }
    }
}