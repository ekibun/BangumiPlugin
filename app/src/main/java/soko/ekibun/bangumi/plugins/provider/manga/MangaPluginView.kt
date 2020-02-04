package soko.ekibun.bangumi.plugins.provider.manga

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.plugin_manga.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.ui.view.ScalableLayoutManager
import soko.ekibun.bangumi.plugins.ui.view.pull.PullLoadLayout
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class MangaPluginView(linePresenter: LinePresenter) : Provider.PluginView(linePresenter, R.layout.plugin_manga) {
    val layoutManager = ScalableLayoutManager(linePresenter.pluginContext)
    val mangaAdapter = MangaAdapter(linePresenter)

    init {
        linePresenter.onStateChangedListener = { state ->
            linePresenter.maskView.visibility = if (state == BottomSheetBehavior.STATE_HIDDEN) View.INVISIBLE else View.VISIBLE
            linePresenter.appbar.visibility = linePresenter.maskView.visibility
        }
        layoutManager.setupWithRecyclerView(view.item_manga) { _, _ ->
            linePresenter.setState(BottomSheetBehavior.STATE_COLLAPSED)
        }
        linePresenter.setHideable(true)
        linePresenter.setPeakRatio(1 / 3f)
        linePresenter.setState(BottomSheetBehavior.STATE_COLLAPSED)
        view.item_manga.adapter = mangaAdapter

        view.item_pull_layout.setOnPullListener(object : PullLoadLayout.OnPullListener {
            override fun onRefreshStart(pullLayout: PullLoadLayout?) {
                val curIndex =
                    linePresenter.episodeAdapter.data.indexOfFirst { it.manga == mangaAdapter.data.lastOrNull()?.ep }
                linePresenter.episodeAdapter.data.getOrNull(curIndex - 1)?.let { ep ->
                    loadEp(ep, true) { view.item_pull_layout.responseRefresh(it) }
                }
            }

            override fun onLoadStart(pullLayout: PullLoadLayout?) {
                val curIndex =
                    linePresenter.episodeAdapter.data.indexOfFirst { it.manga == mangaAdapter.data.lastOrNull()?.ep }
                linePresenter.episodeAdapter.data.getOrNull(curIndex + 1)?.let { ep ->
                    loadEp(ep, false) { view.item_pull_layout.responseload(it) }
                }

            }

        })
    }

    override fun loadEp(episode: Episode) {
        mangaAdapter.setNewData(null)
        view.item_pull_layout.responseRefresh(false)
        view.item_pull_layout.responseload(false)
        loadEp(episode, false) {}
    }

    fun loadEp(episode: Episode, isPrev: Boolean, callback: (Boolean) -> Unit) {
        val ep = episode.manga
        val provider =
            linePresenter.app.lineProvider.getProvider(Provider.TYPE_MANGA, ep?.site ?: "")?.provider as? MangaProvider
        if (ep == null || provider == null) {
            linePresenter.context.runOnUiThread {
                callback(false)
            }
            return
        }
        view.visibility = View.VISIBLE
        layoutManager.reset()
        provider.getManga("getManga", linePresenter.app.jsEngine, ep).enqueue({
            if (isPrev) {
                val curItem = layoutManager.findFirstVisibleItemPosition()
                val curOffset =
                    layoutManager.findViewByPosition(curItem)?.let { layoutManager.getDecoratedTop(it) } ?: 0
                mangaAdapter.addData(0, it)
                layoutManager.scrollToPositionWithOffset(curItem + it.size, curOffset)
            } else mangaAdapter.addData(it)
            callback(true)
        }, {
            callback(false)
        })
    }

}