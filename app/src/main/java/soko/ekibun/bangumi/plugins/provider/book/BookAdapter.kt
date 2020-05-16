package soko.ekibun.bangumi.plugins.provider.book

import android.annotation.SuppressLint
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.HttpException
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.item_page.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.ui.view.SelectableRecyclerView
import soko.ekibun.bangumi.plugins.ui.view.book.BookLayoutManager
import soko.ekibun.bangumi.plugins.util.GlideUtil
import soko.ekibun.bangumi.plugins.util.HttpUtil
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class BookAdapter(val recyclerView: RecyclerView, data: MutableList<BookProvider.PageInfo>? = null) :
    BaseQuickAdapter<BookProvider.PageInfo, BaseViewHolder>(R.layout.item_page, data),
    BookLayoutManager.ScalableAdapter, SelectableRecyclerView.TextSelectionAdapter {
    val requests = HashMap<BookProvider.PageInfo, HttpUtil.HttpRequest>()

    private val referHolder by lazy { createViewHolder(recyclerView, 0) }

    val padding = ResourceUtil.dip2px(App.app.plugin, 24f)

    override fun addData(newData: Collection<BookProvider.PageInfo>) {
        super.addData(wrapData(newData.toList()))
    }

    override fun addData(position: Int, newData: Collection<BookProvider.PageInfo>) {
        super.addData(position, wrapData(newData.toList()))
    }

    private fun getEpTitle(page: BookProvider.PageInfo): String {
        return "${page.ep?.category ?: ""} ${page.ep?.title}".trim()
    }

    var textSize = 0f
    fun updateTextSize(textSize: Float) {
        if (this.textSize == textSize) return
        referHolder.itemView.item_content.textSize = textSize
        this.textSize = textSize
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        val currentIndex = layoutManager?.findFirstVisibleItemPosition() ?: 0
        val currentOffset =
            layoutManager?.findViewByPosition(currentIndex)?.let { layoutManager.getDecoratedTop(it) } ?: 0
        val currentPageInfo = data.getOrNull(currentIndex)
        val currentInfo = currentPageInfo?.rawInfo ?: currentPageInfo
        val currentInfoPos = currentPageInfo?.rawRange?.first ?: 0
        setNewInstance(wrapData(data.map { it.rawInfo ?: it }.distinct()).toMutableList())
        currentInfo?.let { current ->
            (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                data.indexOfFirst {
                    (it.rawInfo ?: it) == current && (it.rawInfo == null || it.rawRange?.second ?: 0 > currentInfoPos)
                }, currentOffset
            )
        }
    }

    private fun wrapData(data: List<BookProvider.PageInfo>): List<BookProvider.PageInfo> {
        val ret = ArrayList<BookProvider.PageInfo>()
        data.forEach { page ->
            if (page.content.isNullOrEmpty()) ret += page
            else {
                val pageWidth =
                    recyclerView.width - referHolder.itemView.content_container.let { it.paddingLeft + it.paddingRight }
                val titleHeight = if (page.index <= 1) referHolder.itemView.item_title.let {
                    getEpTitle(page).let { content ->
                        StaticLayout.Builder.obtain(content, 0, content.length, it.paint, pageWidth)
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(it.lineSpacingExtra, it.lineSpacingMultiplier)
                            .setIncludePad(it.includeFontPadding)
                            .setUseLineSpacingFromFallbacks(it.isFallbackLineSpacing)
                            .setBreakStrategy(it.breakStrategy)
                            .setHyphenationFrequency(it.hyphenationFrequency)
                            .setJustificationMode(it.justificationMode)
                            .build()
                    }.height + it.paddingBottom
                } else 0
                val layout = referHolder.itemView.item_content.let {
                    StaticLayout.Builder.obtain(page.content, 0, page.content.length, it.paint, pageWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(it.lineSpacingExtra, it.lineSpacingMultiplier)
                        .setIncludePad(it.includeFontPadding)
                        .setUseLineSpacingFromFallbacks(it.isFallbackLineSpacing)
                        .setBreakStrategy(it.breakStrategy)
                        .setHyphenationFrequency(it.hyphenationFrequency)
                        .setJustificationMode(it.justificationMode)
                        .build()
                }
                val pageHeight =
                    recyclerView.height - referHolder.itemView.content_container.let { it.paddingTop + it.paddingBottom }
                var lastTextIndex = 0
                var lastLineBottom = 0
                for (i in 1 until layout.lineCount) {
                    val curLineBottom = layout.getLineBottom(i) + titleHeight
                    if (curLineBottom - lastLineBottom < pageHeight) continue
                    val prevLineEndIndex = layout.getLineVisibleEnd(i - 1)
                    ret += BookProvider.PageInfo(
                        content = page.content.substring(lastTextIndex, prevLineEndIndex),
                        ep = page.ep,
                        rawInfo = page,
                        rawRange = Pair(lastTextIndex, prevLineEndIndex)
                    )
                    lastTextIndex = layout.getLineStart(i)
                    lastLineBottom = layout.getLineTop(i) + titleHeight
                }
                ret += BookProvider.PageInfo(
                    content = page.content.substring(lastTextIndex),
                    ep = page.ep,
                    rawInfo = page,
                    rawRange = Pair(lastTextIndex, page.content.length)
                )
            }
        }
        var lastEp: BookProvider.BookEpisode? = null
        var lastIndex = 0
        ret.forEachIndexed { index, page ->
            if (lastEp != page.ep) lastIndex = 0
            lastEp = page.ep
            lastIndex += 1
            page.index = lastIndex
        }
        return ret
    }

    override fun convert(holder: BaseViewHolder, item: BookProvider.PageInfo) {
        holder.itemView.image_sort.text = item.index.toString()
        holder.itemView.item_content.textSize = textSize
        holder.itemView.tag = item
        holder.itemView.loading_text.setOnClickListener {
            if (holder.itemView.tag == item) loadData(holder, item)
        }
        val isHorizontal = (recyclerView.layoutManager as? LinearLayoutManager)?.orientation == RecyclerView.HORIZONTAL
        holder.itemView.content_container.setPadding(
            holder.itemView.content_container.paddingLeft,
            if (isHorizontal || item.index <= 1) holder.itemView.content_container.paddingLeft else 0,
            holder.itemView.content_container.paddingRight,
            if (isHorizontal || data.getOrNull(holder.adapterPosition + 1)?.index ?: 0 <= 1) holder.itemView.content_container.paddingLeft else 0
        )
        holder.itemView.content_container.layoutParams.width = recyclerView.width
        loadData(holder, item)
    }

    @SuppressLint("SetTextI18n")
    private fun loadData(helper: BaseViewHolder, item: BookProvider.PageInfo) {
        helper.itemView.item_image.setImageDrawable(null)
        helper.itemView.content_container.visibility = View.GONE
        helper.itemView.item_loading.visibility = View.VISIBLE
        helper.itemView.loading_progress.visibility = View.VISIBLE
        helper.itemView.loading_text.visibility = View.GONE
        helper.itemView.loading_progress.isIndeterminate = true
        if (!item.content.isNullOrEmpty()) {
            helper.itemView.item_loading.visibility = View.GONE
            helper.itemView.content_container.visibility = View.VISIBLE
            helper.itemView.item_title.visibility = if (item.index <= 1) View.VISIBLE else View.GONE
            helper.itemView.item_title.text = getEpTitle(item)
            helper.itemView.item_content.text = item.content
            return
        }
        val imageRequest = requests[item] ?: if (item.site.isNullOrEmpty()) item.image else null
        if (imageRequest != null) {
            setImage(helper, item, imageRequest)
        } else {
            (LineProvider.getProvider(Provider.TYPE_BOOK, item.site ?: "")?.provider as? BookProvider)
                ?.getImage("${item.site}_${item.index}", App.app.jsEngine, item)
                ?.observeOn(AndroidSchedulers.mainThread())?.subscribe({
                    requests[item] = it
                    setImage(helper, item, it)
                }, {
                    if (helper.itemView.tag == item) {
                        showError(helper, "接口错误\n${it.message}")
                    }
                }) ?: {
                showError(helper, "接口不存在")
            }()
        }
    }

    private fun showError(helper: BaseViewHolder, message: String) {
        helper.itemView.loading_progress.visibility = View.GONE
        helper.itemView.loading_text.visibility = View.VISIBLE
        helper.itemView.loading_text.text = message
    }

    private fun setImage(helper: BaseViewHolder, item: BookProvider.PageInfo, imageRequest: HttpUtil.HttpRequest) {
        helper.itemView.loading_progress.progress = 0
        GlideUtil.loadWithProgress(imageRequest, App.app.host, helper.itemView.item_image, {
            helper.itemView.loading_progress.isIndeterminate = false
            if (helper.itemView.tag == item) helper.itemView.loading_progress.progress = (it * 100).toInt()
        }, {
            if (helper.itemView.tag == item) {
                helper.itemView.item_loading.visibility = View.GONE
            }
        }, { e ->
            val httpException = e.rootCauses.find { it is HttpException } as? HttpException
            showError(
                helper, "加载出错\n${
                httpException?.let { "${it.statusCode} ${it.message}".trim() } ?: e.message
                }"
            )
        })
    }

    override fun isItemScalable(pos: Int, layoutManager: LinearLayoutManager): Boolean {
        return layoutManager.findViewByPosition(pos)?.content_container?.visibility != View.VISIBLE
    }

    val path = Path()
    private val posContent = IntArray(2)
    override fun drawSelection(c: Canvas, view: View, start: Int?, end: Int?, paint: Paint) {
        if (view.content_container.visibility != View.VISIBLE) return
        view.item_content.getLocationInWindow(posContent)
        c.save()
        c.translate(posContent[0] - recyclerView.x, posContent[1] - recyclerView.y)
        drawSelectionImpl(c, view, start, end, paint)

        c.restore()
    }

    private fun drawSelectionImpl(c: Canvas, view: View, start: Int?, end: Int?, paint: Paint) {
        if (start == null && end == null) {
            c.drawRect(Rect(0, 0, view.item_content.width, view.item_content.height), paint)
            return
        }
        val layout = view.item_content.layout ?: return
        layout.getSelectionPath(start ?: 0, end ?: view.item_content.text.length, path)
        if (end == null) {
            val startLine = layout.getLineForOffset(start ?: 0)
            val endLine = layout.getLineForOffset(end ?: view.item_content.text.length)
            path.addRect(
                if (startLine == endLine) layout.getPrimaryHorizontal(start ?: 0) else layout.getLineLeft(endLine),
                layout.getLineTop(endLine).toFloat(),
                layout.getLineLeft(endLine) + layout.width,
                view.item_content.height.toFloat(), Path.Direction.CW
            )
        }
        c.drawPath(path, paint)
    }

    override fun getPosFromPosition(view: View, x: Float, y: Float): Int {
        if (view.content_container.visibility != View.VISIBLE) return -1
        view.item_content.getLocationInWindow(posContent)
        return view.item_content.getOffsetForPosition(
            x - posContent[0] + recyclerView.x,
            y - posContent[1] + recyclerView.y
        )
    }

    val point = Point()
    override fun getHandlePosition(view: View, offset: Int): Point {
        if (view.content_container.visibility != View.VISIBLE) return point.also { it.set(-1000, -1000) }
        val layout = view.item_content.layout ?: return point.also { it.set(-1000, -1000) }
        view.item_content.getLocationInWindow(posContent)
        return point.also {
            it.set(
                (layout.getPrimaryHorizontal(offset) + posContent[0] - recyclerView.x).toInt(),
                (layout.getLineBottom(layout.getLineForOffset(offset)) - view.item_content.lineSpacingExtra + posContent[1] - recyclerView.y).toInt()
            )
        }
    }

    override fun getTextHeight(): Int {
        return referHolder.itemView.item_content.lineHeight
    }

    override fun getSelectionText(startIndex: Int, endIndex: Int, startPos: Int, endPos: Int): String {
        val str = StringBuilder()
        var lastRaw: BookProvider.PageInfo? = null
        var lastStart = 0
        var lastEnd = 0
        for (i in startIndex..endIndex) {
            val item = data.getOrNull(i) ?: break
            if (lastRaw != item.rawInfo) {
                if (lastRaw != null) str.append(lastRaw.content?.substring(lastStart, lastEnd) + '\n')
                lastRaw = item.rawInfo
                lastStart = (item.rawRange?.first ?: 0) + (if (i == startIndex) startPos else 0)
            }
            lastEnd = (item.rawRange?.first ?: 0) + (if (i == endIndex) endPos else 0)
        }
        str.append(lastRaw?.content?.substring(lastStart, lastEnd))
        return str.toString()
    }
}