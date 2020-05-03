package soko.ekibun.bangumi.plugins.subject

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.dialog_episode_list.view.*
import kotlinx.android.synthetic.main.item_episode.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.model.EpisodeCacheModel
import soko.ekibun.bangumi.plugins.service.DownloadService
import soko.ekibun.bangumi.plugins.ui.view.BasePluginDialog

/**
 * 剧集列表对话框
 */
class EpisodeListDialog(private val linePresenter: LinePresenter, val adapter: EpisodeAdapter) :
    BasePluginDialog(linePresenter.activityRef.get()!!, linePresenter.pluginContext, R.layout.dialog_episode_list) {
    companion object {
        /**
         * 显示对话框
         */
        fun showDialog(linePresenter: LinePresenter) {
            val dialog = EpisodeListDialog(
                linePresenter,
                if (linePresenter.selectCache) linePresenter.episodeDetailAdapter else linePresenter.subjectView.episodeDetailAdapter
            )
            dialog.show()
        }
    }

    override val title: String = ""

    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onViewCreated(view: View) {
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
            view.bottom_sheet_container.invalidate()
        }

        adapter.setUpWithRecyclerView(view.bottom_sheet_container)
        view.bottom_sheet_container.layoutManager = LinearLayoutManager(linePresenter.pluginContext)

        adapter.setOnItemChildLongClickListener { _, _, position ->
            val item = adapter.data[position]
            val videoCache = EpisodeCacheModel.getEpisodeCache(item.t!!, linePresenter.subject)
            if (videoCache != null)
                DownloadService.remove(
                    linePresenter.pluginContext,
                    item.t!!,
                    linePresenter.subject
                )
            true
        }

        adapter.setOnItemChildClickListener { _, _, position ->
            val item = adapter.data[position]
            linePresenter.pluginView.downloadEp(item.t!!) { infoString ->
                linePresenter.activityRef.get()?.runOnUiThread {
                    if (infoString.isEmpty()) adapter.notifyItemChanged(position)
                    else adapter.getViewByPosition(position, R.id.item_layout)?.let {
                        it.item_download_info.text = infoString
                    }
                }
            }
        }

        val paddingTop = view.bottom_sheet.paddingTop
        val paddingBottom = view.bottom_sheet_container.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.bottom_sheet.setPadding(
                view.bottom_sheet.paddingLeft,
                paddingTop + insets.systemWindowInsetTop,
                view.bottom_sheet.paddingRight,
                view.bottom_sheet.paddingBottom
            )
            view.bottom_sheet_container.setPadding(
                view.bottom_sheet_container.paddingLeft,
                view.bottom_sheet_container.paddingTop,
                view.bottom_sheet_container.paddingRight,
                paddingBottom + insets.systemWindowInsetBottom
            )
            insets
        }
    }
}
