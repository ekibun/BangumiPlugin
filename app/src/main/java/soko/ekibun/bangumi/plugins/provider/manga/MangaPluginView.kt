package soko.ekibun.bangumi.plugins.provider.manga

import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.plugin_manga.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.ui.view.ScalableLayoutManager
import soko.ekibun.bangumi.plugins.ui.view.pull.PullLoadLayout

class MangaPluginView(linePresenter: LinePresenter) : Provider.PluginView(linePresenter, R.layout.plugin_manga) {
    val layoutManager = ScalableLayoutManager(linePresenter.pluginContext)
    val mangaAdapter = MangaAdapter(linePresenter)

    init {
        linePresenter.proxy.subjectPresenter.subjectView.onStateChangedListener = { state ->
            linePresenter.proxy.item_mask.visibility =
                if (state == BottomSheetBehavior.STATE_HIDDEN) View.INVISIBLE else View.VISIBLE
            linePresenter.proxy.app_bar.visibility = linePresenter.proxy.item_mask.visibility
        }
        layoutManager.setupWithRecyclerView(view.item_manga) { _, _ ->
            linePresenter.proxy.subjectPresenter.subjectView.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        linePresenter.proxy.subjectPresenter.subjectView.behavior.isHideable = true
        linePresenter.proxy.subjectPresenter.subjectView.peakRatio = 1 / 3f
        linePresenter.proxy.subjectPresenter.subjectView.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        view.item_manga.adapter = mangaAdapter
        view.item_manga.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                updateProgress()
            }
        })

        view.item_pull_layout.setOnPullListener(object : PullLoadLayout.OnPullListener {
            override fun onRefreshStart(pullLayout: PullLoadLayout?) {
                val curIndex =
                    linePresenter.episodeAdapter.data.indexOfFirst { it.manga == mangaAdapter.data.firstOrNull()?.ep }
                linePresenter.episodeAdapter.data.getOrNull(curIndex - 1)?.let { ep ->
                    loadEp(ep, true) { view.item_pull_layout.responseRefresh(it) }
                }?: view.item_pull_layout.responseRefresh(false)
            }

            override fun onLoadStart(pullLayout: PullLoadLayout?) {
                val curIndex =
                    linePresenter.episodeAdapter.data.indexOfFirst { it.manga == mangaAdapter.data.lastOrNull()?.ep }
                linePresenter.episodeAdapter.data.getOrNull(curIndex + 1)?.let { ep ->
                    loadEp(ep, false) { view.item_pull_layout.responseLoad(it) }
                }?: view.item_pull_layout.responseLoad(false)

            }

        })
    }

    override fun loadEp(episode: Episode) {
        mangaAdapter.setNewData(null)
        view.item_pull_layout.responseRefresh(false)
        view.item_pull_layout.responseLoad(false)
        view.visibility = View.VISIBLE
        loadEp(episode, false) {}
    }

    private fun updateProgress(){
        mangaAdapter.data.getOrNull(layoutManager.findFirstVisibleItemPosition())?.let {
            linePresenter.proxy.subjectPresenter.subjectView.collapsibleAppBarHelper.setTitle(null,
                "${it.ep?.sort}-${it.index}/${mangaAdapter.data.lastOrNull { l -> l.ep == it.ep }?.index}", null)
        }
    }

    fun loadEp(episode: Episode, isPrev: Boolean, callback: (Boolean) -> Unit) {
        val ep = episode.manga
        val provider =
            LineProvider.getProvider(Provider.TYPE_MANGA, ep?.site ?: "")?.provider as? MangaProvider
        if (ep == null || provider == null) {
            callback(false)
            return
        }
        linePresenter.proxy.subjectPresenter.subjectView.collapsibleAppBarHelper.setTitle(null, ep.sort, null)
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
            Toast.makeText(linePresenter.pluginContext, it.message, Toast.LENGTH_LONG).show()
            callback(false)
        })
    }

}