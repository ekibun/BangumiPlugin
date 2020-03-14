package soko.ekibun.bangumi.plugins.provider.book

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.controller_book.view.*
import kotlinx.android.synthetic.main.item_page.view.*
import kotlinx.android.synthetic.main.plugin_book.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.EpisodeCache
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.service.DownloadService
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.ui.view.ScalableLayoutManager
import soko.ekibun.bangumi.plugins.ui.view.pull.PullLoadLayout
import soko.ekibun.bangumi.plugins.util.AppUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil

class BookPluginView(val linePresenter: LinePresenter) : Provider.PluginView(linePresenter, R.layout.plugin_book) {
    val baseTextSize = 16f

    val layoutManager = ScalableLayoutManager(linePresenter.pluginContext) { child, lm ->
        child.item_content?.let {
            if (it.visibility != View.VISIBLE) return@let
            it.width = lm.recyclerView.width
            it.textSize = baseTextSize * ((lm.scale - 1) / 2 + 1)
            it.translationX = lm.offsetX.toFloat()
        }
        child.item_loading?.translationX = lm.offsetX + lm.width * (1 - lm.scale) / 2
    }
    val mangaAdapter = BookAdapter()

    companion object {
        const val tapScrollRange = 1 / 4f
        const val tapScrollRatio = 1 / 2f
    }

    private fun showControl(show: Boolean) {
        if (view.control_container.visibility == if (show) View.VISIBLE else View.INVISIBLE) return
        if (show) updateProgress()
        view.control_container.visibility = if (show) View.VISIBLE else View.INVISIBLE
        view.control_container.animation = AnimationUtils.loadAnimation(
            linePresenter.pluginContext,
            if (show) R.anim.move_in_bottom else R.anim.move_out_bottom
        )
    }

    private var inited = false
    private fun init() {
        if (inited) return
        inited = true

        view.btn_next.setOnClickListener {
            val visibleItem = mangaAdapter.data.getOrNull(layoutManager.findFirstVisibleItemPosition())
            val curIndex = linePresenter.episodeAdapter.data.indexOfFirst { it.book == visibleItem?.ep }
            linePresenter.episodeAdapter.data.getOrNull(curIndex + 1)?.let { ep -> loadEp(ep) } ?: {
                Toast.makeText(App.app.plugin, "没有下一章了", Toast.LENGTH_LONG).show()
            }()
        }
        view.btn_prev.setOnClickListener {
            val visibleItem = mangaAdapter.data.getOrNull(layoutManager.findFirstVisibleItemPosition())
            val curIndex = linePresenter.episodeAdapter.data.indexOfFirst { it.book == visibleItem?.ep }
            linePresenter.episodeAdapter.data.getOrNull(curIndex - 1)?.let { ep -> loadEp(ep) } ?: {
                Toast.makeText(App.app.plugin, "没有上一章了", Toast.LENGTH_LONG).show()
            }()
        }
        view.read_progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val visiblePos = layoutManager.findFirstVisibleItemPosition()
                val visibleIndex = (mangaAdapter.data.getOrNull(visiblePos)?.index ?: 1) - 1
                val visibleViewAdditionProgress = layoutManager.findViewByPosition(visiblePos)?.let {
                    Math.max(layoutManager.getDecoratedMeasuredHeight(it) / layoutManager.recyclerView.height - 1, 0)
                } ?: 0
                layoutManager.scrollToPositionWithOffset(
                    visiblePos + when {
                        progress < visibleIndex -> progress - visibleIndex
                        progress > visibleIndex + visibleViewAdditionProgress ->
                            progress - visibleIndex - visibleViewAdditionProgress
                        else -> 0
                    },
                    if (progress > visibleIndex && progress <= visibleIndex + visibleViewAdditionProgress)
                        -(progress - visibleIndex) * layoutManager.recyclerView.height
                    else 0
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        view.btn_title.setOnClickListener {
            linePresenter.proxy.subjectPresenter.subjectView.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

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
                    else -> showControl(view.control_container.visibility != View.VISIBLE)
                }
            })
        { view, index ->
            showControl(false)
            val systemUiVisibility = view.systemUiVisibility
            val url = mangaAdapter.data[index].image?.url ?: return@setupWithRecyclerView
            val dialog = AlertDialog.Builder(linePresenter.pluginContext)
                .setTitle(url)
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
                        linePresenter.episodeAdapter.data.indexOfFirst { it.book == mangaAdapter.data.firstOrNull()?.ep }
                    linePresenter.episodeAdapter.data.getOrNull(curIndex - 1)?.let { ep ->
                        loadEp(ep, true) { view.item_pull_layout.responseRefresh(it) }
                    } ?: view.item_pull_layout.responseRefresh(false)
                }

                override fun onLoadStart(pullLayout: PullLoadLayout?) {
                    val curIndex =
                        linePresenter.episodeAdapter.data.indexOfFirst { it.book == mangaAdapter.data.lastOrNull()?.ep }
                    linePresenter.episodeAdapter.data.getOrNull(curIndex + 1)?.let { ep ->
                        loadEp(ep, false) { view.item_pull_layout.responseLoad(it) }
                    } ?: view.item_pull_layout.responseLoad(false)

                }

            })
    }

    override fun loadEp(episode: Episode) {
        init()
        mangaAdapter.setNewData(null)
        view.item_pull_layout.responseRefresh(false)
        view.item_pull_layout.responseLoad(false)
        view.visibility = View.VISIBLE
        loadEp(episode, false) {}
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgress() {
        view.read_progress.isEnabled = false
        val visiblePos = layoutManager.findFirstVisibleItemPosition()
        mangaAdapter.data.getOrNull(visiblePos)?.let {
            val last = mangaAdapter.data.lastOrNull { l -> l.ep == it.ep } ?: it
            view.read_progress.isEnabled = true
            view.read_progress.progress = it.index - 1
            view.read_progress.max = last.index - 1
            layoutManager.findViewByPosition(visiblePos)?.let {
                val top = layoutManager.getDecoratedTop(it)
                val height = layoutManager.getDecoratedMeasuredHeight(it)
                view.read_progress.max += Math.max(height / layoutManager.recyclerView.height - 1, 0)
                view.read_progress.progress -= top / layoutManager.recyclerView.height
            }
            view.btn_title.text = "${it.ep?.title}-${it.index}/${last.index}"
            linePresenter.proxy.subjectPresenter.subjectView.collapsibleAppBarHelper.setTitle(
                null,
                "${it.ep?.title}-${it.index}/${last.index}", null
            )
        }
    }

    fun loadEp(episode: Episode, isPrev: Boolean, callback: (Boolean) -> Unit) {
        val ep = episode.book
        val cache = App.app.episodeCacheModel.getEpisodeCache(
            episode,
            linePresenter.subject
        )?.cache() as? EpisodeCache.BookCache
        if (cache != null) {
            cache.request.forEach { kv ->
                mangaAdapter.requests[cache.pages[kv.key]] = kv.value
            }
            if (isPrev) {
                val curItem = layoutManager.findFirstVisibleItemPosition()
                val curOffset =
                    layoutManager.findViewByPosition(curItem)?.let { layoutManager.getDecoratedTop(it) } ?: 0
                mangaAdapter.addData(0, cache.pages)
                layoutManager.scrollToPositionWithOffset(curItem + cache.pages.size, curOffset)
            } else mangaAdapter.addData(cache.pages)
            updateProgress()
            callback(true)
            return
        }

        val provider =
            App.app.lineProvider.getProvider(Provider.TYPE_BOOK, ep?.site ?: "")?.provider as? BookProvider
        if (ep == null || provider == null) {
            callback(false)
            return
        }
        linePresenter.proxy.subjectPresenter.subjectView.collapsibleAppBarHelper.setTitle(null, ep.title, null)
        layoutManager.reset()
        provider.getPages("getManga", App.app.jsEngine, ep).enqueue({
            if (isPrev) {
                val curItem = layoutManager.findFirstVisibleItemPosition()
                val curOffset =
                    layoutManager.findViewByPosition(curItem)?.let { layoutManager.getDecoratedTop(it) } ?: 0
                mangaAdapter.addData(0, it)
                layoutManager.scrollToPositionWithOffset(curItem + it.size, curOffset)
            } else mangaAdapter.addData(it)
            updateProgress()
            callback(true)
        }, {
            Toast.makeText(App.app.plugin, it.message, Toast.LENGTH_LONG).show()
            updateProgress()
            callback(false)
        })
    }

    override fun downloadEp(episode: Episode, updateInfo: (String) -> Unit) {
        val ep = episode.book
        val cache = App.app.episodeCacheModel.getEpisodeCache(episode, linePresenter.subject)
        if (cache != null) {
            DownloadService.download(
                linePresenter.pluginContext,
                episode,
                linePresenter.proxy.subjectPresenter.subject,
                cache
            )
            return
        }
        val provider =
            App.app.lineProvider.getProvider(Provider.TYPE_BOOK, ep?.site ?: "")?.provider as? BookProvider
        if (ep == null || provider == null) return
        updateInfo("获取图片列表")
        provider.getPages("getManga", App.app.jsEngine, ep).enqueue({
            updateInfo("创建下载请求")
            DownloadService.download(
                linePresenter.pluginContext, episode, linePresenter.proxy.subjectPresenter.subject, EpisodeCache(
                    episode, Provider.TYPE_BOOK,
                    JsonUtil.toJson(
                        EpisodeCache.BookCache(
                            it, HashMap(), HashMap()
                        )
                    )
                )
            )
        }, {
            updateInfo("获取图片列表出错")
        })
    }
}