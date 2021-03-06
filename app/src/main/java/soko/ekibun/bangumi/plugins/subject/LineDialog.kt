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
import kotlinx.android.synthetic.main.dialog_add_line.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.action.ActionPresenter
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.ui.view.BasePluginDialog
import soko.ekibun.bangumi.plugins.util.JsonUtil

class LineDialog(private val linePresenter: LinePresenter) :
    BasePluginDialog(linePresenter.activityRef.get()!!, linePresenter.pluginContext, R.layout.base_dialog) {
    override val title: String get() = if (info == null) "添加线路" else "编辑线路"

    companion object {
        fun showDialog(
            linePresenter: LinePresenter,
            info: LineInfo?,
            callback: (LineInfo?, LineInfo?) -> Unit
        ) {
            val dialog = LineDialog(linePresenter)
            dialog.info = info
            dialog.callback = callback
            dialog.show()
        }
    }

    var info: LineInfo? = null
    lateinit var callback: (LineInfo?, LineInfo?) -> Unit
    override fun onViewCreated(view: View) {
        LayoutInflater.from(pluginContext).inflate(R.layout.dialog_add_line, view.findViewById(R.id.layout_content))

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
            val provider = view.item_line.tag as? ProviderInfo ?: return@setOnClickListener
            callback(
                info,
                LineInfo(
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
    val emptyProvider = ProviderInfo("", 0, "链接")

    private fun updateProvider(view: View) {
        view.item_line.setOnClickListener {
            val popList = ListPopupWindow(context)
            popList.anchorView = view.item_line
            val providers = LineProvider.getProviderList(linePresenter.type)
            val providerList: ArrayList<ProviderInfo> = ArrayList(providers)

            providerList.add(0, emptyProvider)
            providerList.add(ProviderInfo("", 0, "添加..."))
            providerList.add(ProviderInfo("", 0, "导出..."))
            providerList.add(ProviderInfo("", 0, "导入..."))
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
                            LineProvider.addProvider(it)
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
                                ProviderInfo.toUrl(providers)
                            )
                        )
                        Toast.makeText(view.context, "数据已导出至剪贴板", Toast.LENGTH_SHORT).show()
                    }
                    providerList.size - 1 -> linePresenter.subscribe {
                        val data = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (!ActionPresenter.importProvider(context, data)) {
                            //import
                            JsonUtil.toEntity<List<JsonObject>>(data)?.let { list ->
                                list.forEach { obj ->
                                    ActionPresenter.addProvider(
                                        context,
                                        JsonUtil.toEntity<ProviderInfo>(JsonUtil.toJson(obj))!!.also { info ->
                                            if (!obj.has("type")) {
                                                info.code = JsonUtil.toJson(obj)
                                                info.type = linePresenter.type
                                            }
                                        })
                                }
                                Toast.makeText(view.context, "已添加${list.size}个接口", Toast.LENGTH_SHORT).show()
                            } ?: JsonUtil.toEntity<JsonObject>(data)?.let { obj ->
                                ActionPresenter.addProvider(
                                    context,
                                    JsonUtil.toEntity<ProviderInfo>(JsonUtil.toJson(obj))!!.also { info ->
                                        if (!obj.has("type")) {
                                            info.code = JsonUtil.toJson(obj)
                                            info.type = linePresenter.type
                                        }
                                    })
                                Toast.makeText(view.context, "已添加1个接口", Toast.LENGTH_SHORT).show()
                            } ?: {
                                Toast.makeText(view.context, "剪贴板没有数据", Toast.LENGTH_SHORT).show()
                            }()
                        }

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
                    LineProvider.removeProvider(info)
                    if (it != null) LineProvider.addProvider(it)
                    updateProvider(view)
                }
                true
            }
        }
        (view.item_line?.tag as? LineInfo)?.let { updateInfo(view, it) }
    }

    private fun updateInfo(view: View, info: LineInfo) {
        val provider = LineProvider.getProvider(linePresenter.type, info.site) ?: emptyProvider
        view.item_line.text = provider.title
        view.item_line.tag = provider
        view.item_video_id.setText(info.id)
        view.item_video_title.setText(info.title)
        view.item_video_extra.setText(info.extra)
    }
}