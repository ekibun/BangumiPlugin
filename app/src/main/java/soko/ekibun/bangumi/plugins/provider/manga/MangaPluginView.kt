package soko.ekibun.bangumi.plugins.provider.manga

import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.item_image.view.*
import kotlinx.android.synthetic.main.plugin_manga.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.ui.view.ScalableLayoutManager
import soko.ekibun.bangumi.plugins.ui.view.pull.PullLoadLayout
import soko.ekibun.bangumi.plugins.util.AppUtil

class MangaPluginView(val linePresenter: LinePresenter) : Provider.PluginView(linePresenter, R.layout.plugin_manga) {
    val layoutManager = ScalableLayoutManager(linePresenter.pluginContext)
    val mangaAdapter = MangaAdapter()

    companion object {
        const val tapScrollRange = 1 / 4f
        const val tapScrollRatio = 1 / 2f
    }

    init {
        linePresenter.proxy.onBackListener = {
            val behavior = linePresenter.proxy.subjectPresenter.subjectView.behavior
            if (behavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                true
            } else false
        }

        linePresenter.proxy.subjectPresenter.subjectView.onStateChangedListener = { state ->
            val maskVisibility = if (state == BottomSheetBehavior.STATE_HIDDEN) View.INVISIBLE else View.VISIBLE
            linePresenter.proxy.item_mask.visibility = maskVisibility
            linePresenter.proxy.app_bar.visibility = maskVisibility
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener {
            linePresenter.activityRef.get()?.window?.decorView?.systemUiVisibility =
                if (linePresenter.proxy.subjectPresenter.subjectView.behavior.state != BottomSheetBehavior.STATE_HIDDEN)
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or (if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION else 0)
                else (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
        layoutManager.setupWithRecyclerView(view.item_manga,
            { _, y ->
                val h = view.item_manga.height
                when {
                    y < h * tapScrollRange -> view.item_manga.smoothScrollBy(0, -(h * tapScrollRatio).toInt())
                    h - y < h * tapScrollRange -> view.item_manga.smoothScrollBy(0, (h * tapScrollRatio).toInt())
                    else -> linePresenter.proxy.subjectPresenter.subjectView.behavior.state =
                        BottomSheetBehavior.STATE_COLLAPSED
                }
            })
        { view, index ->
            val systemUiVisibility = view.systemUiVisibility
            val dialog = AlertDialog.Builder(linePresenter.pluginContext)
                .setTitle(mangaAdapter.data[index].url)
                .setItems(arrayOf("分享"))
                { _, _ ->
                    linePresenter.activityRef.get()
                        ?.let { AppUtil.shareDrawable(it, view.item_image.drawable ?: return@setItems) }
                }.setOnDismissListener {
                    view.systemUiVisibility = systemUiVisibility
                }.create()
            dialog.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            dialog.show()
        }
        linePresenter.proxy.subjectPresenter.subjectView.let {
            it.behavior.isHideable = true
            it.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            it.peakRatio = 1 / 3f
        }
        view.item_manga.adapter = mangaAdapter
        view.item_manga.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    updateProgress()
                }
            })

        view.item_pull_layout.setOnPullListener(
            object : PullLoadLayout.OnPullListener {
                override fun onRefreshStart(pullLayout: PullLoadLayout?) {
                    val curIndex =
                        linePresenter.episodeAdapter.data.indexOfFirst { it.manga == mangaAdapter.data.firstOrNull()?.ep }
                    linePresenter.episodeAdapter.data.getOrNull(curIndex - 1)?.let { ep ->
                        loadEp(ep, true) { view.item_pull_layout.responseRefresh(it) }
                    } ?: view.item_pull_layout.responseRefresh(false)
                }

                override fun onLoadStart(pullLayout: PullLoadLayout?) {
                    val curIndex =
                        linePresenter.episodeAdapter.data.indexOfFirst { it.manga == mangaAdapter.data.lastOrNull()?.ep }
                    linePresenter.episodeAdapter.data.getOrNull(curIndex + 1)?.let { ep ->
                        loadEp(ep, false) { view.item_pull_layout.responseLoad(it) }
                    } ?: view.item_pull_layout.responseLoad(false)

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

    private fun updateProgress() {
        mangaAdapter.data.getOrNull(layoutManager.findFirstVisibleItemPosition())?.let {
            linePresenter.proxy.subjectPresenter.subjectView.collapsibleAppBarHelper.setTitle(
                null,
                "${it.ep?.sort}-${it.index}/${mangaAdapter.data.lastOrNull { l -> l.ep == it.ep }?.index}", null
            )
        }
    }

    fun loadEp(episode: Episode, isPrev: Boolean, callback: (Boolean) -> Unit) {
        val ep = episode.manga
        val provider =
            App.app.lineProvider.getProvider(Provider.TYPE_MANGA, ep?.site ?: "")?.provider as? MangaProvider
        if (ep == null || provider == null) {
            callback(false)
            return
        }
        linePresenter.proxy.subjectPresenter.subjectView.collapsibleAppBarHelper.setTitle(null, ep.sort, null)
        layoutManager.reset()
        provider.getManga("getManga", App.app.jsEngine, ep).enqueue({
            if (isPrev) {
                val curItem = layoutManager.findFirstVisibleItemPosition()
                val curOffset =
                    layoutManager.findViewByPosition(curItem)?.let { layoutManager.getDecoratedTop(it) } ?: 0
                mangaAdapter.addData(0, it)
                layoutManager.scrollToPositionWithOffset(curItem + it.size, curOffset)
            } else mangaAdapter.addData(it)
            callback(true)
        }, {
            Toast.makeText(App.app.plugin, it.message, Toast.LENGTH_LONG).show()
            callback(false)
        })
    }

}