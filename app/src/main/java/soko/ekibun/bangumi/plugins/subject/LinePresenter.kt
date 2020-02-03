package soko.ekibun.bangumi.plugins.subject

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.subject_episode.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.ui.provider.ProviderActivity
import soko.ekibun.bangumi.plugins.util.AppUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil
import soko.ekibun.bangumi.plugins.util.ReflectUtil
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class LinePresenter(val context: Activity, val pluginContext: Context) {
    val lineProvider by lazy { App.from(pluginContext).lineProvider }
    val lineInfoModel by lazy { App.from(pluginContext).lineInfoModel }

    val subjectPresenter = ReflectUtil.getVal(context, "subjectPresenter")!!
    val subjectView = ReflectUtil.getVal(subjectPresenter, "subjectView")!!
    val detail = ReflectUtil.getVal(subjectView, "detail") as View
    val epLayout = detail.findViewById<LinearLayout>(ResourceUtil.getId(context, "item_episodes"))

    fun subject() = ReflectUtil.convert<Subject>(ReflectUtil.convert<Subject>(ReflectUtil.getVal(subjectPresenter, "subject")))!!

    val epView = LayoutInflater.from(pluginContext).inflate(R.layout.subject_episode, null)
    val mySubjectView = SubjectView(pluginContext, epView)

    fun showEpisodeListDialog(){
        ReflectUtil.invoke(subjectPresenter, "showEpisodeListDialog", arrayOf())
    }

    fun showEpisodeDialog(id: Int){
        ReflectUtil.invoke(subjectPresenter, "showEpisodeDialog", arrayOf(Int::class.java), id)
    }

    init {
        // 添加自己的view
        epView.episode_list.adapter = mySubjectView.episodeAdapter
        epView.episode_list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        epLayout.addView(epView, 2, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        mySubjectView.updateEpisode(subject())
        epView.btn_detail.setOnClickListener {
            showEpisodeListDialog()
        }
        mySubjectView.episodeAdapter.setOnItemChildClickListener { _, _, position ->
            showEpisodeDialog(mySubjectView.episodeAdapter.data[position].id)
        }
        // subjectRefreshListener
        ReflectUtil.invoke(
            subjectPresenter,
            "setSubjectRefreshListener",
            arrayOf(Function1::class.java),
            { data: Any? ->
                if (data?.javaClass?.name in arrayOf(
                        "soko.ekibun.bangumi.api.bangumi.bean.Subject", List::class.java.name
                    )) {
                    mySubjectView.updateEpisode(subject())
                    refreshLines()
                }
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
        if(type.isEmpty()) return
        loadProviderCallback = callback
        val intent = Intent(pluginContext, ProviderActivity::class.java)
        intent.putExtra(ProviderActivity.EXTRA_PROVIDER_TYPE, type)
        info?.let { intent.putExtra(ProviderActivity.EXTRA_PROVIDER_INFO, JsonUtil.toJson(info)) }
        context.startActivityForResult(intent, AppUtil.REQUEST_PROVIDER)
    }

    var type = ""

    fun refreshLines() {
        val subject = subject()
        val infos = lineInfoModel.getInfos(subject)

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
            lineInfoModel.saveInfos(subject, infos)
        }

        val editLines = { info: LineInfoModel.LineInfo? ->
            LineDialog.showDialog(this,
                info
            ) { oldInfo, newInfo ->
                setProvider(oldInfo, newInfo)
                refreshLines()
            }
        }

        val defaultLine = infos?.getDefaultProvider()
        context.runOnUiThread {
            val showPlugin = type.isNotEmpty()
            epLayout.getChildAt(0).visibility = if(showPlugin) View.GONE else View.VISIBLE
            epLayout.getChildAt(1).visibility = if(showPlugin) View.GONE else View.VISIBLE
            epView.visibility = if(showPlugin) View.VISIBLE else View.GONE
            epLayout.visibility = if(showPlugin || subject.eps?.size?:0 > 0) View.VISIBLE else View.GONE

            if (defaultLine != null) {
                val provider = lineProvider.getProvider(type, defaultLine.site)
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
                            lineInfoModel.saveInfos(subject, infos)
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
//                // 加载eps
//                provider?.let {
//                    subjectView.updateEpisode(null)
//                    Log.v("load", defaultLine.toString())
//                    it.getEpisode("loadEps", jsEngine, defaultLine).enqueue({ eps ->
//                        Log.v("load", eps.toString())
//                        subjectView.updateEpisode(eps)
//                    }, { e ->
//                        subjectView.updateEpisode(null, e)
//                    })
//                }
            } else {
                epView.episodes_line_site.visibility = View.GONE
                epView.episodes_line_id.text = "+ 添加线路"
                epView.episodes_line.setOnClickListener {
                    editLines(null)
                }
            }
        }
    }
}