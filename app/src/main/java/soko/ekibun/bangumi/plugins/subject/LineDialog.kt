package soko.ekibun.bangumi.plugins.subject

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.ListPopupWindow
import android.widget.Toast
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.base_dialog.view.*
import kotlinx.android.synthetic.main.dialog_add_line.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.ui.view.BasePluginDialog
import soko.ekibun.bangumi.plugins.util.JsonUtil

class LineDialog(private val linePresenter: LinePresenter) :
    BasePluginDialog(linePresenter.activityRef.get()!!, linePresenter.pluginContext, R.layout.base_dialog) {
    override val title: String get() = if (info == null) "添加线路" else "编辑线路"

    companion object {
        fun showDialog(
            linePresenter: LinePresenter,
            info: LineInfoModel.LineInfo?,
            callback: (LineInfoModel.LineInfo?, LineInfoModel.LineInfo?) -> Unit
        ) {
            val dialog = LineDialog(linePresenter)
            dialog.info = info
            dialog.callback = callback
            dialog.show()
        }
    }

    var info: LineInfoModel.LineInfo? = null
    lateinit var callback: (LineInfoModel.LineInfo?, LineInfoModel.LineInfo?) -> Unit
    override fun onViewCreated(view: View) {
        LayoutInflater.from(pluginContext).inflate(R.layout.dialog_add_line, view.layout_content)

        val paddingBottom = view.item_buttons.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.item_buttons.setPadding(
                view.item_buttons.paddingLeft,
                view.item_buttons.paddingTop,
                view.item_buttons.paddingRight,
                paddingBottom + insets.systemWindowInsetBottom
            )
            insets.consumeSystemWindowInsets()
        }

        view.item_line.text = emptyProvider.title
        view.item_line.tag = emptyProvider
        updateProvider(view)
        info?.let { updateInfo(view, it) }

        view.item_delete.visibility = if (info == null) View.GONE else View.VISIBLE
        view.item_delete.setOnClickListener {
            AlertDialog.Builder(context).setMessage("删除这个线路？").setPositiveButton("确定") { _: DialogInterface, _: Int ->
                callback(info, null)
                dismiss()
            }.show()
        }

        view.item_search.setOnClickListener {
            SearchDialog.showDialog(linePresenter, callback)
            dismiss()
        }

        view.item_ok.setOnClickListener {
            val provider = view.item_line.tag as? LineProvider.ProviderInfo ?: return@setOnClickListener
            callback(
                info,
                LineInfoModel.LineInfo(
                    site = provider.site,
                    id = view.item_video_id.text.toString(),
                    title = view.item_video_title.text.toString(),
                    extra = view.item_video_extra.text.toString()
                )
            )
            dismiss()
        }
    }

    val clipboardManager by lazy { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val emptyProvider = LineProvider.ProviderInfo("", 0, "链接")

    private fun updateProvider(view: View) {
        view.item_line.setOnClickListener {
            val popList = ListPopupWindow(context)
            popList.anchorView = view.item_line
            val providerList: ArrayList<LineProvider.ProviderInfo> =
                ArrayList(App.app.lineProvider.providerList.values.filter { it.type == linePresenter.type })

            providerList.add(0, emptyProvider)
            providerList.add(LineProvider.ProviderInfo("", 0, "添加..."))
            providerList.add(LineProvider.ProviderInfo("", 0, "导出..."))
            providerList.add(LineProvider.ProviderInfo("", 0, "导入..."))
            popList.setAdapter(ProviderAdapter(context, providerList))
            popList.isModal = true
            popList.show()
            popList.listView?.setOnItemClickListener { _, _, position, _ ->
                popList.dismiss()
                when (position) {
                    providerList.size - 3 -> {
                        //doAdd
                        linePresenter.loadProvider(linePresenter.type, null) {
                            if (it == null) return@loadProvider
                            App.app.lineProvider.addProvider(it)
                            view.item_line.text = it.title
                            view.item_line.tag = it
                            updateProvider(view)
                        }
                    }
                    providerList.size - 2 -> {
                        //export
                        clipboardManager.setPrimaryClip(
                            ClipData.newPlainText(
                                "videoplayer.providerInfo",
                                JsonUtil.toJson(App.app.lineProvider.providerList.values)
                            )
                        )
                        Toast.makeText(view.context, "数据已导出至剪贴板", Toast.LENGTH_SHORT).show()
                    }
                    providerList.size - 1 -> {
                        val addProvider = { obj: JsonObject ->
                            val providerInfo =
                                JsonUtil.toEntity<LineProvider.ProviderInfo>(JsonUtil.toJson(obj))!!.also { info ->
                                    if (!obj.has("type")) {
                                        info.code = JsonUtil.toJson(obj)
                                        info.type = linePresenter.type
                                    }
                                }
                            val oldProvider = App.app.lineProvider.getProvider(linePresenter.type, providerInfo.site)
                            if (oldProvider != null)
                                AlertDialog.Builder(context).setMessage("接口 ${providerInfo.title}(${providerInfo.site}) 与现有接口 ${oldProvider.title}(${oldProvider.site}) 重复")
                                    .setPositiveButton("替换") { _: DialogInterface, _: Int ->
                                        App.app.lineProvider.addProvider(providerInfo)
                                    }.setNegativeButton("取消") { _: DialogInterface, _: Int -> }.show()
                            else App.app.lineProvider.addProvider(providerInfo)
                        }
                        //inport
                        JsonUtil.toEntity<List<JsonObject>>(
                            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        )?.let { list ->
                            list.forEach { addProvider(it) }
                            Toast.makeText(view.context, "已添加${list.size}个接口", Toast.LENGTH_SHORT).show()
                        } ?: JsonUtil.toEntity<JsonObject>(
                            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        )?.let {
                            addProvider(it)
                            Toast.makeText(view.context, "已添加1个接口", Toast.LENGTH_SHORT).show()
                        } ?: {
                            Toast.makeText(view.context, "剪贴板没有数据", Toast.LENGTH_SHORT).show()
                        }()
                    }
                    else -> {
                        val provider = providerList[position]
                        view.item_line.text = provider.title
                        view.item_line.tag = provider
                    }
                }
            }
            popList.listView?.setOnItemLongClickListener { _, _, position, _ ->
                popList.dismiss()
                if (providerList.size - position < 4 || position == 0) return@setOnItemLongClickListener false
                //edit
                val info = providerList[position]
                linePresenter.loadProvider(linePresenter.type, info) {
                    App.app.lineProvider.removeProvider(info)
                    if (it != null) App.app.lineProvider.addProvider(it)
                    updateProvider(view)
                }
                true
            }
        }
        (view.item_line?.tag as? LineInfoModel.LineInfo)?.let { updateInfo(view, it) }
    }

    private fun updateInfo(view: View, info: LineInfoModel.LineInfo) {
        val provider = App.app.lineProvider.getProvider(linePresenter.type, info.site) ?: emptyProvider
        view.item_line.text = provider.title
        view.item_line.tag = provider
        view.item_video_id.setText(info.id)
        view.item_video_title.setText(info.title)
        view.item_video_extra.setText(info.extra)
    }
}