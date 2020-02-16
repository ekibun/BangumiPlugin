package soko.ekibun.bangumi.plugins.subject

import android.annotation.SuppressLint
import android.view.View
import android.widget.ListPopupWindow
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.dialog_search.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.JsEngine
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.ui.view.BasePluginDialog

class SearchDialog(private val linePresenter: LinePresenter) :
    BasePluginDialog(linePresenter.activityRef.get()!!, linePresenter.pluginContext, R.layout.dialog_search) {
    override val title: String get() = "搜索"

    companion object {
        fun showDialog(
            linePresenter: LinePresenter,
            callback: (LineInfoModel.LineInfo?, LineInfoModel.LineInfo?) -> Unit
        ) {
            val dialog = SearchDialog(linePresenter)
            dialog.callback = callback
            dialog.show()
        }
    }

    lateinit var callback: (LineInfoModel.LineInfo?, LineInfoModel.LineInfo?) -> Unit
    override fun onViewCreated(view: View) {
        val subject = linePresenter.proxy.subjectPresenter.subject
        view.item_search_key.setText(subject.displayName)
        view.list_search.layoutManager = LinearLayoutManager(context)

        val adapter = SearchLineAdapter(linePresenter)
        adapter.lines = App.app.lineInfoModel.getInfos(subject)
        adapter.setOnItemClickListener { _, _, position ->
            val item = adapter.data[position]
            val exist =
                adapter.lines?.providers?.firstOrNull { it.site == item.site && it.id == item.id } != null
            if (exist) {
                Toast.makeText(context, "线路已存在，长按编辑此线路", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }
            callback(null, adapter.data[position])
            adapter.lines = App.app.lineInfoModel.getInfos(subject)
            adapter.notifyItemChanged(position)
        }
        adapter.setOnItemLongClickListener { _, _, position ->
            LineDialog.showDialog(linePresenter, adapter.data[position], callback)
            dismiss()
            true
        }
        view.list_search.adapter = adapter

        view.item_line.text = emptyProvider.title
        view.item_line.tag = emptyProvider
        updateProvider(view)

        val behavior = BottomSheetBehavior.from(view.bottom_sheet)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            @SuppressLint("SwitchIntDef")
            override fun onStateChanged(bottomSheet: View, @BottomSheetBehavior.State newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) dismiss()
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) { /* no-op */
            }
        })
        behavior.isHideable = true
        view.post {
            behavior.peekHeight = view.height * 2 / 3
        }

        val paddingTop = view.bottom_sheet.paddingTop
        val paddingBottom = view.list_search.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.bottom_sheet.setPadding(
                view.bottom_sheet.paddingLeft,
                paddingTop + insets.systemWindowInsetTop,
                view.bottom_sheet.paddingRight,
                view.bottom_sheet.paddingBottom
            )
            view.list_search.setPadding(
                view.list_search.paddingLeft,
                view.list_search.paddingTop,
                view.list_search.paddingRight,
                paddingBottom + insets.systemWindowInsetBottom
            )
            insets.consumeSystemWindowInsets()
        }

        val searchCall =
            ArrayList<Pair<LineProvider.ProviderInfo, JsEngine.ScriptTask<List<LineInfoModel.LineInfo>>>>()
        view.item_search.setOnClickListener {
            adapter.setNewData(null)
            val key = view.item_search_key.text.toString()
            searchCall.forEach { it.second.cancel(true) }
            searchCall.clear()

            val jsEngine = App.app.jsEngine
            val provider = view.item_line.tag as? LineProvider.ProviderInfo ?: emptyProvider
            searchCall.addAll(if (provider == emptyProvider) {
                ArrayList(App.app.lineProvider.providerList.values.filter { it.type == linePresenter.type && !it.provider?.search.isNullOrEmpty() })
                    .map {
                        Pair(it, it.provider!!.search("search_${it.site}", jsEngine, key))
                    }
            } else listOf(provider.let { Pair(it, it.provider!!.search("search_${it.site}", jsEngine, key)) })
            )

            searchCall.forEach {
                it.second.enqueue({ lines ->
                    adapter.addData(lines)
                }, { e ->
                    Toast.makeText(context, "${it.first.title}: ${e.message}", Toast.LENGTH_LONG).show()
                })
            }
        }
    }

    private fun updateProvider(view: View) {
        val popList = ListPopupWindow(view.context)
        popList.anchorView = view.item_line
        val providers: ArrayList<LineProvider.ProviderInfo> =
            ArrayList(App.app.lineProvider.providerList.values.filter { it.type == linePresenter.type && !it.provider?.search.isNullOrEmpty() })
        providers.add(0, emptyProvider)
        popList.setAdapter(ProviderAdapter(view.context, providers))
        popList.isModal = true

        view.item_line.setOnClickListener {
            popList.show()
            popList.listView?.setOnItemClickListener { _, _, position, _ ->
                popList.dismiss()
                view.item_line.text = providers[position].title
                view.item_line.tag = providers[position]
            }
            popList.listView?.setOnItemLongClickListener { _, _, position, _ ->
                popList.dismiss()
                if (position == 0) return@setOnItemLongClickListener false
                //edit
                val info = providers[position]
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
    }

    private val emptyProvider = LineProvider.ProviderInfo("", 0, "所有线路")
}