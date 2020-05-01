package soko.ekibun.bangumi.plugins.provider.book

import android.annotation.SuppressLint
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
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
import soko.ekibun.bangumi.plugins.ui.view.BookLayoutManager
import soko.ekibun.bangumi.plugins.ui.view.PullLoadLayout
import soko.ekibun.bangumi.plugins.util.AppUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil

class BookPluginView(val linePresenter: LinePresenter) : Provider.PluginView(linePresenter, R.layout.plugin_book) {

    val layoutManager = BookLayoutManager(linePresenter.pluginContext) { child, lm ->
        child.content_container?.let {
            if (it.visibility != View.VISIBLE) return@let
            it.layoutParams.width = lm.recyclerView.width
            it.translationX = lm.offsetX.toFloat()
        }
        child.item_loading?.translationX = lm.offsetX + lm.width * (1 - lm.scale) / 2
    }
    val bookAdapter = BookAdapter(view.item_manga)

    companion object {
        const val tapScrollRange = 1 / 4f
        const val tapScrollRatio = 1 / 2f
    }

    val textSizeList = arrayOf(10f, 12f, 15f, 18f, 22f)
    val sp = PreferenceManager.getDefaultSharedPreferences(App.app.plugin)
    private fun updateConfigAndProgress() {
        val direction = sp.getInt("plugin_book_direction", RecyclerView.VERTICAL)
        view.btn_direction.text = if (direction == RecyclerView.VERTICAL) "纵向" else "横向"
        if (layoutManager.orientation != direction) {
            val curIndex = layoutManager.findFirstVisibleItemPosition()
            val curOffset = layoutManager.findViewByPosition(curIndex)?.let { layoutManager.getDecoratedTop(it) } ?: 0
            layoutManager.orientation = direction
            bookAdapter.notifyDataSetChanged()
            layoutManager.scrollToPositionWithOffset(curIndex, curOffset)
        }
        view.btn_direction.setOnClickListener {
            sp.edit().putInt(
                "plugin_book_direction",
                if (direction == RecyclerView.VERTICAL) RecyclerView.HORIZONTAL else RecyclerView.VERTICAL
            ).apply()
            updateConfigAndProgress()
        }
        val textSizeIndex = sp.getInt("plugin_book_text_size", 2)
        val textSize = textSizeList[textSizeIndex]
        bookAdapter.updateTextSize(textSize)
        view.btn_size_plus.setOnClickListener {
            if (textSizeIndex + 1 < textSizeList.size) {
                sp.edit().putInt("plugin_book_text_size", textSizeIndex + 1).apply()
                updateConfigAndProgress()
            }
        }
        view.btn_size_minus.setOnClickListener {
            if (textSizeIndex - 1 >= 0) {
                sp.edit().putInt("plugin_book_text_size", textSizeIndex - 1).apply()
                updateConfigAndProgress()
            }
        }
        updateProgress()
    }

    var cancelOnDown = false
    private fun showControl(show: Boolean) {
        if (cancelOnDown || view.control_container.visibility == if (show) View.VISIBLE else View.INVISIBLE) return
        cancelOnDown = true
        if (show) updateConfigAndProgress()
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
            val visibleItem = bookAdapter.data.getOrNull(layoutManager.findFirstVisibleItemPosition())
            val curIndex = linePresenter.episodeAdapter.data.indexOfFirst { it.book == visibleItem?.ep }
            linePresenter.episodeAdapter.data.getOrNull(curIndex + 1)?.let { ep -> loadEp(ep) } ?: {
                Toast.makeText(App.app.plugin, "没有下一章了", Toast.LENGTH_LONG).show()
            }()
        }
        view.btn_prev.setOnClickListener {
            val visibleItem = bookAdapter.data.getOrNull(layoutManager.findFirstVisibleItemPosition())
            val curIndex = linePresenter.episodeAdapter.data.indexOfFirst { it.book == visibleItem?.ep }
            linePresenter.episodeAdapter.data.getOrNull(curIndex - 1)?.let { ep -> loadEp(ep) } ?: {
                Toast.makeText(App.app.plugin, "没有上一章了", Toast.LENGTH_LONG).show()
            }()
        }
        view.read_progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val visiblePos = layoutManager.findFirstVisibleItemPosition()
                val visibleIndex = (bookAdapter.data.getOrNull(visiblePos)?.index ?: 1) - 1
                layoutManager.scrollToPositionWithOffset(visiblePos + progress - visibleIndex, 0)
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
            { x, y ->
                if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                    val h = view.item_manga.height
                    when {
                        y < h * tapScrollRange -> view.item_manga.smoothScrollBy(0, -(h * tapScrollRatio).toInt())
                        h - y < h * tapScrollRange -> view.item_manga.smoothScrollBy(0, (h * tapScrollRatio).toInt())
                        else -> showControl(true)
                    }
                } else {
                    val w = view.item_manga.width
                    when {
                        x < w * tapScrollRange -> layoutManager.snapToTarget(layoutManager.currentPos.toInt() - 1)
                        w - x < w * tapScrollRange -> layoutManager.snapToTarget(layoutManager.currentPos.toInt() + 1)
                        else -> showControl(true)
                    }
                }

            }, { v, index ->
                val systemUiVisibility = v.systemUiVisibility
                val url = bookAdapter.data[index].image?.url ?: return@setupWithRecyclerView
                val dialog = AlertDialog.Builder(linePresenter.pluginContext)
                    .setTitle(url)
                    .setItems(arrayOf("分享"))
                    { _, _ ->
                        linePresenter.activityRef.get()
                            ?.let { AppUtil.shareDrawable(it, v.item_image.drawable ?: return@setItems) }
                    }.setOnDismissListener {
                        v.systemUiVisibility = systemUiVisibility
                    }.create()
                dialog.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
                dialog.show()
            }) {
            if (it.action == MotionEvent.ACTION_DOWN) {
                cancelOnDown = false
                showControl(false)
            }
        }
        linePresenter.proxy.subjectPresenter.subjectView.let {
            it.behavior.isHideable = true
            it.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            it.peakRatio = 1 / 3f
        }
        view.item_manga.adapter = bookAdapter
        updateConfigAndProgress()

        view.item_manga.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (view.control_container.visibility == View.VISIBLE) updateProgress()
                }
            })

        view.item_pull_layout.listener = object : PullLoadLayout.PullLoadListener {
            override fun onRefresh() {
                val curIndex =
                    linePresenter.episodeAdapter.data.indexOfFirst { it.book == bookAdapter.data.firstOrNull()?.ep }
                linePresenter.episodeAdapter.data.getOrNull(curIndex - 1)?.let { ep ->
                    loadEp(ep, true) { view.item_pull_layout.response(it) }
                } ?: view.item_pull_layout.response(false)
            }

            override fun onLoad() {
                val curIndex =
                    linePresenter.episodeAdapter.data.indexOfFirst { it.book == bookAdapter.data.lastOrNull()?.ep }
                linePresenter.episodeAdapter.data.getOrNull(curIndex + 1)?.let { ep ->
                    loadEp(ep, false) { view.item_pull_layout.response(it) }
                } ?: view.item_pull_layout.response(false)
            }
        }
    }

    override fun loadEp(episode: Episode) {
        init()
        bookAdapter.setNewData(null)
        view.book_loading.visibility = View.VISIBLE
        view.item_pull_layout.response(false)
        view.visibility = View.VISIBLE
        loadEp(episode, false) {
            view.book_loading.visibility = View.INVISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgress() {
        view.read_progress.isEnabled = false
        val visiblePos = layoutManager.findFirstVisibleItemPosition()
        bookAdapter.data.getOrNull(visiblePos)?.let {
            val last = bookAdapter.data.lastOrNull { l -> l.ep == it.ep } ?: it
            view.read_progress.isEnabled = true
            view.read_progress.progress = it.index - 1
            view.read_progress.max = last.index - 1
            view.btn_title.text = "${it.ep?.title}-${it.index}/${last.index}"
        }
    }

    private fun updatePage(pages: List<BookProvider.PageInfo>, isPrev: Boolean) {
        if (isPrev) {
            val curIndex = layoutManager.findFirstVisibleItemPosition()
            val curItem = bookAdapter.getItem(curIndex)
            val curOffset = layoutManager.findViewByPosition(curIndex)?.let { layoutManager.getDecoratedTop(it) } ?: 0
            bookAdapter.addData(0, pages.toMutableList())
            layoutManager.scrollToPositionWithOffset(bookAdapter.data.indexOf(curItem), curOffset)
        } else bookAdapter.addData(pages.toMutableList())
    }

    fun loadEp(episode: Episode, isPrev: Boolean, callback: (Boolean) -> Unit) {
        val ep = episode.book
        val cache = App.app.episodeCacheModel.getEpisodeCache(
            episode,
            linePresenter.subject
        )?.cache() as? EpisodeCache.BookCache
        if (cache != null) {
            cache.request.forEach { kv ->
                bookAdapter.requests[cache.pages[kv.key]] = kv.value
            }
            updatePage(cache.pages, isPrev)
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
        layoutManager.reset()
        provider.getPages("getManga", App.app.jsEngine, ep).enqueue({
            updatePage(it, isPrev)
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