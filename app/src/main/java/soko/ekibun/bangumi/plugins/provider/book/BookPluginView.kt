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
import soko.ekibun.bangumi.plugins.model.EpisodeCacheModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.model.cache.EpisodeCache
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.service.DownloadService
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.ui.view.PullLoadLayout
import soko.ekibun.bangumi.plugins.ui.view.book.GestureTouchListener
import soko.ekibun.bangumi.plugins.ui.view.book.ScalableLayoutManager
import soko.ekibun.bangumi.plugins.ui.view.book.layout.PageLayoutManager
import soko.ekibun.bangumi.plugins.ui.view.book.layout.RollLayoutManager
import soko.ekibun.bangumi.plugins.util.AppUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil

class BookPluginView(val linePresenter: LinePresenter) : Provider.PluginView(linePresenter, R.layout.plugin_book) {

    val bookLayoutManagers = listOf(
        RollLayoutManager(view.item_manga, linePresenter.pluginContext),
        PageLayoutManager(view.item_manga, linePresenter.pluginContext, rtl = false),
        PageLayoutManager(view.item_manga, linePresenter.pluginContext, rtl = true)
    )
    val layoutManager get() = view.item_manga.layoutManager as ScalableLayoutManager
    val bookAdapter = BookAdapter(view.item_manga)

    companion object {
        const val tapScrollRange = 1 / 4f
        const val tapScrollRatio = 1 / 2f
    }

    val textSizeList = arrayOf(10f, 12f, 15f, 18f, 22f)
    val sp = PreferenceManager.getDefaultSharedPreferences(App.app.plugin)
    private fun updateConfigAndProgress() {
        val direction = sp.getInt("plugin_book_direction", RecyclerView.VERTICAL)
        view.btn_direction.text = arrayOf("卷轴", "普通", "日漫").getOrNull(direction) ?: "卷轴"
        if (bookLayoutManagers.indexOf(layoutManager) != direction) {
            val curIndex = layoutManager.findFirstVisibleItemPosition()
            val curOffset = layoutManager.findViewByPosition(curIndex)?.let { layoutManager.getDecoratedTop(it) } ?: 0
            view.item_manga.layoutManager = bookLayoutManagers.getOrNull(direction) ?: layoutManager
            view.item_manga.adapter = bookAdapter
            layoutManager.scrollToPositionWithOffset(curIndex, curOffset)
        }
        view.btn_direction.setOnClickListener {
            sp.edit().putInt(
                "plugin_book_direction",
                (direction + 1) % bookLayoutManagers.size
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
        view.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
            if (l != ol || t != ot || r != or || b != ob) bookAdapter.updateTextSize(force = true)
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
        val touchListener = GestureTouchListener()
        view.item_manga.setOnTouchListener(touchListener)
        touchListener.listeners.add(object : GestureTouchListener.GestureCallback() {
            override fun onTouch(e: MotionEvent) {
                if (e.action == MotionEvent.ACTION_DOWN) {
                    cancelOnDown = false
                    showControl(false)
                }
            }

            override fun onSingleTapConfirmed(e: MotionEvent) {
                if (view.item_manga.clearSelect()) return
                val layoutManager = layoutManager
                if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                    val h = view.item_manga.height
                    when {
                        e.y < h * tapScrollRange -> view.item_manga.smoothScrollBy(0, -(h * tapScrollRatio).toInt())
                        h - e.y < h * tapScrollRange -> view.item_manga.smoothScrollBy(0, (h * tapScrollRatio).toInt())
                        else -> showControl(true)
                    }
                } else if (layoutManager is PageLayoutManager) {
                    val w = view.item_manga.width
                    when {
                        e.x < w * tapScrollRange -> layoutManager.snapToTarget(layoutManager.currentPos.toInt() - 1)
                        w - e.x < w * tapScrollRange -> layoutManager.snapToTarget(layoutManager.currentPos.toInt() + 1)
                        else -> showControl(true)
                    }
                }
            }

            override fun onLongPress(e: MotionEvent) {
                val v = view.item_manga.findChildViewUnder(e.x, e.y) ?: return
                val index = view.item_manga.getChildAdapterPosition(v)
                val url = bookAdapter.data[index].image?.url
                if (url == null) {
//                    val actionMode = view.item_manga.startActionMode(SelectableActionMode(), ActionMode.TYPE_FLOATING)
                    if (!view.item_manga.isActive) view.item_manga.startSelect(e.x, e.y)
                    return
                }
                val systemUiVisibility = v.systemUiVisibility
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
            }
        })
        view.item_manga.layoutManager = bookLayoutManagers[0]
        ScalableLayoutManager.setupWithRecyclerView(view.item_manga, touchListener)
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
        bookAdapter.setNewInstance(null)
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
            bookAdapter.addProviderData(0, pages.toMutableList())
            layoutManager.scrollToPositionWithOffset(bookAdapter.data.indexOf(curItem), curOffset)
        } else bookAdapter.addProviderData(pages.toMutableList())
    }

    fun loadEp(episode: Episode, isPrev: Boolean, callback: (Boolean) -> Unit) {
        val ep = episode.book
        val cache = EpisodeCacheModel.getEpisodeCache(
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
            LineProvider.getProvider(Provider.TYPE_BOOK, ep?.site ?: "")?.provider as? BookProvider
        if (ep == null || provider == null) {
            callback(false)
            return
        }
        layoutManager.reset()
        linePresenter.subscribe({
            Toast.makeText(App.app.plugin, it.message, Toast.LENGTH_LONG).show()
            updateProgress()
            callback(false)
        }, key = "getManga") {
            val pages = provider.getPages("getManga", ep)
            updatePage(pages, isPrev)
            updateProgress()
            callback(true)
        }
    }

    override fun downloadEp(episode: Episode, updateInfo: (String) -> Unit) {
        val ep = episode.book
        linePresenter.subscribe({
            updateInfo("获取页面信息出错")
        }, key = "getManga_${ep?.id}") {
            val cache = EpisodeCacheModel.getEpisodeCache(episode, linePresenter.subject)
            if (cache != null) {
                DownloadService.download(
                    linePresenter.pluginContext,
                    episode,
                    linePresenter.proxy.subjectPresenter.subject,
                    cache
                )
                return@subscribe
            }
            val provider =
                LineProvider.getProvider(Provider.TYPE_BOOK, ep?.site ?: "")?.provider as? BookProvider
            if (ep == null || provider == null) return@subscribe
            updateInfo("获取页面信息")
            val pages = provider.getPages("getManga_${ep.id}", ep)
            updateInfo("创建下载请求")
            DownloadService.download(
                linePresenter.pluginContext, episode, linePresenter.proxy.subjectPresenter.subject,
                EpisodeCache(
                    episode, Provider.TYPE_BOOK,
                    JsonUtil.toJson(
                        EpisodeCache.BookCache(
                            pages, HashMap(), HashMap()
                        )
                    )
                )
            )
        }
    }
}