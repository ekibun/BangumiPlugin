package soko.ekibun.bangumi.plugins.subject

import android.annotation.SuppressLint
import android.view.View
import android.widget.ListPopupWindow
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.dialog_search.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.ui.view.BasePluginDialog

class SearchDialog(private val linePresenter: LinePresenter) :
    BasePluginDialog(linePresenter.activityRef.get()!!, linePresenter.pluginContext, R.layout.dialog_search) {
    override val title: String get() = "搜索"

    companion object {
        fun showDialog(
            linePresenter: LinePresenter,
            callback: (LineInfo?, LineInfo?) -> Unit
        ) {
            val dialog = SearchDialog(linePresenter)
            dialog.callback = callback
            dialog.show()
        }
    }

    lateinit var callback: (LineInfo?, LineInfo?) -> Unit
    override fun onViewCreated(view: View) {
        val subject = linePresenter.proxy.subjectPresenter.subject
        view.item_search_key.setText(subject.displayName)
        view.list_search.layoutManager = LinearLayoutManager(context)

        val adapter = SearchLineAdapter(linePresenter)
        adapter.lines = LineInfoModel.getInfo(subject)
        adapter.setOnItemClickListener { _, _, position ->
            val item = adapter.data[position]
            val exist =
                adapter.lines?.providers?.firstOrNull { it.site == item.site && it.id == item.id } != null
            if (exist) {
                Toast.makeText(context, "线路已存在，长按编辑此线路", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }
            callback(null, adapter.data[position])
            adapter.lines = LineInfoModel.getInfo(subject)
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
        view.item_search.setOnClickListener {
            adapter.setNewInstance(null)
            val key = view.item_search_key.text.toString()
            linePresenter.cancel { it.startsWith("search_") }
            val provider = view.item_line.tag as? ProviderInfo ?: emptyProvider
            (if (provider == emptyProvider) {
                LineProvider.getProviderList(linePresenter.type)
                    .filter { !it.provider?.search.isNullOrEmpty() }
            } else listOf(provider)).forEach { line ->
                linePresenter.subscribe(key = "search_${line.site}") {
                    adapter.addData(line.provider!!.search("search_${line.site}", key))
                }
            }
        }
    }

    private fun updateProvider(view: View) {
        val popList = ListPopupWindow(view.context)
        popList.anchorView = view.item_line
        val providers: ArrayList<ProviderInfo> =
            ArrayList(LineProvider.getProviderList(linePresenter.type).filter { !it.provider?.search.isNullOrEmpty() })
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
    }

    private val emptyProvider = ProviderInfo("", 0, "所有线路")
}