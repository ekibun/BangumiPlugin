package soko.ekibun.bangumi.plugins.subject

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.subject_episode.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.JsEngine
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.manga.MangaProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.ui.provider.ProviderActivity
import soko.ekibun.bangumi.plugins.util.AppUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil
import soko.ekibun.bangumi.plugins.util.ReflectUtil
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class LinePresenter(val context: Activity, val pluginContext: Context) {
    val app by lazy { App.from(pluginContext) }
//    val lineProvider by lazy { App.from(pluginContext).lineProvider }
//    val lineInfoModel by lazy { App.from(pluginContext).lineInfoModel }
//    val jsEngine by lazy { App.from(pluginContext).jsEngine }

    val episodeAdapter = SmallEpisodeAdapter()
    val emptyView = {
        val view = TextView(context)
        view.gravity = Gravity.CENTER
        view.height = ResourceUtil.dip2px(context, 65f)
        view
    }()

    val subjectPresenter = ReflectUtil.getVal(context, "subjectPresenter")!!
    val subjectView = ReflectUtil.getVal(subjectPresenter, "subjectView")!!
    val detail = ReflectUtil.getVal(subjectView, "detail") as View
    val behavior = ReflectUtil.getVal(subjectView, "behavior")!!
    val epLayout = detail.findViewById<LinearLayout>(ResourceUtil.getId(context, "item_episodes"))

    fun subject() =
        ReflectUtil.convert<Subject>(ReflectUtil.convert<Subject>(ReflectUtil.getVal(subjectPresenter, "subject")))!!

    val epView = LayoutInflater.from(pluginContext).inflate(R.layout.subject_episode, null)
    val mySubjectView = SubjectView(this, epView)

    fun showEpisodeListDialog() {
        ReflectUtil.invoke(subjectPresenter, "showEpisodeListDialog", arrayOf())
    }

    fun showEpisodeDialog(id: Int) {
        ReflectUtil.invoke(subjectPresenter, "showEpisodeDialog", arrayOf(Int::class.java), id)
    }

    val pluginContainer = context.findViewById<FrameLayout>(ResourceUtil.getId(context, "item_plugin"))
    val maskView = context.findViewById<View>(ResourceUtil.getId(context, "item_mask"))
    val appbar = context.findViewById<View>(ResourceUtil.getId(context, "app_bar"))

    @BottomSheetBehavior.State
    fun getState(): Int {
        return ReflectUtil.getVal(behavior, "state") as Int
    }

    fun setState(@BottomSheetBehavior.State state: Int) {
        ReflectUtil.invoke(behavior, "setState", arrayOf(Int::class.java), state)
    }

    fun setPeakRatio(peakRatio: Float) {
        ReflectUtil.invoke(subjectView, "setPeakRatio", arrayOf(Float::class.java), peakRatio)
    }

    fun setPeakMargin(peakMargin: Float) {
        ReflectUtil.invoke(subjectView, "setPeakMargin", arrayOf(Float::class.java), peakMargin)
    }

    fun setHideable(hideable: Boolean) {
        ReflectUtil.invoke(behavior, "setHideable", arrayOf(Boolean::class.java), hideable)
    }

    val pluginView: Provider.PluginView by lazy {
        ReflectUtil.newInstance(Provider.providers[type]!!).createPluginView(this)
    }
    var onStateChangedListener = { state: Int -> }

    init {
        episodeAdapter.emptyView = emptyView
        // 添加自己的view
        epView.episode_list.adapter = mySubjectView.episodeAdapter
        epView.episode_list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        epLayout.addView(
            epView,
            2,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        mySubjectView.updateEpisode(subject())
        epView.btn_detail.setOnClickListener {
            showEpisodeListDialog()
        }
        // subjectRefreshListener
        ReflectUtil.invoke(
            subjectPresenter,
            "setSubjectRefreshListener",
            arrayOf(Function1::class.java),
            { data: Any? ->
                if (data?.javaClass?.name in arrayOf(
                        "soko.ekibun.bangumi.api.bangumi.bean.Subject", List::class.java.name
                    )
                ) {
                    mySubjectView.updateEpisode(subject())
                    refreshLines()
                }
            })
        // onStateChangedListener
        ReflectUtil.invoke(
            subjectView,
            "setOnStateChangedListener",
            arrayOf(kotlin.jvm.functions.Function1::class.java),
            onActivityResult@{ state: Int ->
                onStateChangedListener(state)
            })
        // onActivityResult
        ReflectUtil.invoke(
            context,
            "setOnActivityResultListener",
            arrayOf(kotlin.jvm.functions.Function3::class.java),
            onActivityResult@{ requestCode: Int, resultCode: Int, data: Intent? ->
                //                if (requestCode == AppUtil.REQUEST_FILE_CODE && resultCode == AppCompatActivity.RESULT_OK) {//文件
//                    val uri = data?.data ?: return@onActivityResult
//                    val path = StorageUtil.getRealPathFromUri(pluginContext, uri)
//                    loadFileCallback?.invoke(path)
//                }
                if (requestCode == AppUtil.REQUEST_PROVIDER && resultCode == AppCompatActivity.RESULT_OK) {//Provider
                    loadProviderCallback?.invoke(
                        JsonUtil.toEntity<LineProvider.ProviderInfo>(
                            data?.getStringExtra(
                                ProviderActivity.EXTRA_PROVIDER_INFO
                            ) ?: ""
                        )
                    )
                }
            })
    }

//    private var loadFileCallback: ((String?) -> Unit)? = null
//    fun loadFile(fileType: String, callback: (String?) -> Unit) {
//        loadFileCallback = callback
//        if (!AppUtil.checkStorage(context)) return
//        val intent = Intent()
//        intent.type = fileType
//        intent.action = Intent.ACTION_GET_CONTENT
//        context.startActivityForResult(intent, AppUtil.REQUEST_FILE_CODE)
//    }

    private var loadProviderCallback: ((LineProvider.ProviderInfo?) -> Unit)? = null
    fun loadProvider(type: String, info: LineProvider.ProviderInfo?, callback: (LineProvider.ProviderInfo?) -> Unit) {
        if (type.isEmpty()) return
        loadProviderCallback = callback
        val intent = Intent(pluginContext, ProviderActivity::class.java)
        intent.putExtra(ProviderActivity.EXTRA_PROVIDER_TYPE, type)
        info?.let { intent.putExtra(ProviderActivity.EXTRA_PROVIDER_INFO, JsonUtil.toJson(info)) }
        context.startActivityForResult(intent, AppUtil.REQUEST_PROVIDER)
    }

    var type = ""

    var epCall: Pair<LineProvider.ProviderInfo, JsEngine.ScriptTask<List<MangaProvider.MangaEpisode>>>? = null
    fun refreshLines() {
        val subject = subject()
        val infos = app.lineInfoModel.getInfos(subject)

        episodeAdapter.setOnItemChildClickListener { _, _, position ->
            pluginView.loadEp(episodeAdapter.data[position])
        }
        mySubjectView.episodeAdapter.setOnItemChildClickListener { _, _, position ->
            infos?.getDefaultProvider()?.let {
                pluginView.loadEp(mySubjectView.episodeAdapter.data[position])
            } ?: showEpisodeDialog(mySubjectView.episodeAdapter.data[position].id)
        }

        type = Provider.getProviderType(subject)
        Log.v("plugin", "type $type")

        val setProvider = setProvider@{ info: LineInfoModel.LineInfo?, newInfo: LineInfoModel.LineInfo? ->
            if (infos == null) return@setProvider
            val position =
                infos.providers.indexOfFirst { it.id == info?.id && it.site == info.site }
            when {
                newInfo == null -> if (position >= 0) {
                    infos.providers.removeAt(position)
                    infos.defaultProvider -= if (infos.defaultProvider > position) 1 else 0
                    infos.defaultProvider = Math.max(0, Math.min(infos.providers.size - 1, infos.defaultProvider))
                }
                position >= 0 -> infos.providers[position] = newInfo
                else -> infos.providers.add(newInfo)
            }
            app.lineInfoModel.saveInfos(subject, infos)
        }

        val editLines = { info: LineInfoModel.LineInfo? ->
            LineDialog.showDialog(
                this,
                info
            ) { oldInfo, newInfo ->
                setProvider(oldInfo, newInfo)
                refreshLines()
            }
        }

        val defaultLine = infos?.getDefaultProvider()
        context.runOnUiThread {
            val showPlugin = type.isNotEmpty()
            epLayout.getChildAt(0).visibility = if (showPlugin) View.GONE else View.VISIBLE
            epLayout.getChildAt(1).visibility = if (showPlugin) View.GONE else View.VISIBLE
            epView.visibility = if (showPlugin) View.VISIBLE else View.GONE
            epLayout.visibility = if (showPlugin || subject.eps?.size ?: 0 > 0) View.VISIBLE else View.GONE

            if (defaultLine != null) {
                val provider = app.lineProvider.getProvider(type, defaultLine.site)
                epView.episodes_line_id.text = if (defaultLine.title.isEmpty()) defaultLine.id else defaultLine.title
                epView.episodes_line_site.backgroundTintList =
                    ColorStateList.valueOf(((provider?.color ?: 0) + 0xff000000).toInt())
                epView.episodes_line_site.visibility = View.VISIBLE
                epView.episodes_line_site.text = provider?.title ?: { if (defaultLine.site == "") "线路" else "错误接口" }()
                epView.episodes_line.setOnClickListener {
                    val popList = ListPopupWindow(pluginContext)
                    popList.anchorView = epView.episodes_line
                    val lines = ArrayList(infos.providers)
                    lines.add(LineInfoModel.LineInfo("", "添加线路"))
                    val adapter = LineAdapter(type, pluginContext, lines)
                    adapter.selectIndex = infos.defaultProvider
                    popList.setAdapter(adapter)
                    popList.isModal = true
                    popList.show()
                    popList.listView?.setOnItemClickListener { _, _, position, _ ->
                        popList.dismiss()
                        Log.v("pos", "click: $position")
                        if (position == lines.size - 1) editLines(null)
                        else {
                            infos.defaultProvider = position
                            app.lineInfoModel.saveInfos(subject, infos)
                            epCall = null
                            refreshLines()
                        }
                    }
                    popList.listView?.setOnItemLongClickListener { _, _, position, _ ->
                        popList.dismiss()
                        if (position == lines.size - 1) editLines(null)
                        else editLines(lines[position])
                        true
                    }
                }
                // 加载eps
                if (epCall?.first != provider) (provider?.provider as? MangaProvider)?.let {
                    emptyView.text = "加载中..."
                    episodeAdapter.setNewData(null)
                    epCall = provider to it.getEpisode("loadEps", app.jsEngine, defaultLine)
                    epCall?.second?.enqueue({ eps ->
                        if (epCall?.first == provider)
                            episodeAdapter.setNewData(eps.map { mangaEpisode ->
                                val sort = Regex("(\\d+)").find(
                                    mangaEpisode.sort
                                )?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f
                                Episode(
                                    sort = sort,
                                    progress = if (sort > subject.ep_status) Episode.PROGRESS_REMOVE else Episode.PROGRESS_WATCH,
                                    manga = mangaEpisode
                                )
                            })
                        (epView.episode_list.layoutManager as LinearLayoutManager)
                            .scrollToPositionWithOffset(
                                episodeAdapter.data.indexOfLast { it.sort <= subject.ep_status },
                                0
                            )
                    }, { e ->
                        emptyView.text = e.message
                    })
                }
                episodeAdapter.data.forEachIndexed { index, episode ->
                    episode.progress =
                        if (episode.sort > subject.ep_status) Episode.PROGRESS_REMOVE else Episode.PROGRESS_WATCH
                }
                episodeAdapter.notifyDataSetChanged()
            } else {
                epView.episodes_line_site.visibility = View.GONE
                epView.episodes_line_id.text = "+ 添加线路"
                epView.episodes_line.setOnClickListener {
                    editLines(null)
                }
                emptyView.text = "点击线路加载剧集"
                episodeAdapter.setNewData(null)
            }
            if (type == Provider.TYPE_MANGA) {
                if (epView.episode_list.adapter != episodeAdapter)
                    epView.episode_list.adapter = episodeAdapter
            } else if (epView.episode_list.adapter != mySubjectView.episodeAdapter)
                epView.episode_list.adapter = mySubjectView.episodeAdapter
        }
    }
}