package soko.ekibun.bangumi.plugins.provider.book

import android.annotation.SuppressLint
import android.graphics.*
import android.text.*
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.HttpException
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_page.view.*
import kotlinx.coroutines.launch
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.ui.view.SelectableRecyclerView
import soko.ekibun.bangumi.plugins.ui.view.book.BookLayoutManager
import soko.ekibun.bangumi.plugins.ui.view.book.LetterSpacingSpan
import soko.ekibun.bangumi.plugins.util.GlideUtil
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class BookAdapter(private val recyclerView: RecyclerView, data: MutableList<PageInfo>? = null) :
    BaseQuickAdapter<BookAdapter.PageInfo, BaseViewHolder>(R.layout.item_page, data),
    BookLayoutManager.ScalableAdapter, SelectableRecyclerView.TextSelectionAdapter {
    val requests = HashMap<BookProvider.PageInfo, Provider.HttpRequest>()

    private val referHolder by lazy { createViewHolder(recyclerView, 0) }

    val padding = ResourceUtil.dip2px(App.app.plugin, 24f)

    fun addProviderData(newData: Collection<BookProvider.PageInfo>) {
        super.addData(wrapData(newData.toList()))
    }

    fun addProviderData(position: Int, newData: Collection<BookProvider.PageInfo>) {
        super.addData(position, wrapData(newData.toList()))
    }

    private fun getEpTitle(ep: BookProvider.BookEpisode?): String {
        return "${ep?.category ?: ""} ${ep?.title}".trim()
    }

    var textSize = 0f
    fun updateTextSize(textSize: Float = this.textSize, force: Boolean = false) {
        if (!force && this.textSize == textSize) return
        referHolder.itemView.item_content.textSize = textSize
        this.textSize = textSize
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        val currentIndex = layoutManager?.findFirstVisibleItemPosition() ?: 0
        val currentOffset =
            layoutManager?.findViewByPosition(currentIndex)?.let { layoutManager.getDecoratedTop(it) } ?: 0
        val currentPageInfo = data.getOrNull(currentIndex)
        val currentInfo = currentPageInfo?.rawInfo
        val currentInfoPos = currentPageInfo?.rawRange?.first ?: 0
        setNewInstance(wrapData(data.map { it.rawInfo }.distinct()).toMutableList())
        currentInfo?.let { current ->
            (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                data.indexOfFirst {
                    it.rawInfo == current && (it.rawRange == null || it.rawRange?.last ?: 0 > currentInfoPos)
                }, currentOffset
            )
        }
    }

    data class PageInfo(
        val site: String? = null,
        val image: Provider.HttpRequest? = null,
        val content: CharSequence? = null,
        var ep: BookProvider.BookEpisode? = null,
        var index: Int = 0,
        var rawInfo: BookProvider.PageInfo,
        var rawRange: IntRange? = null
    )

    val tempPaint = TextPaint()
    var widthArray = FloatArray(1)
    private fun wrapData(data: List<BookProvider.PageInfo>): List<PageInfo> {
        val ret = ArrayList<PageInfo>()
        data.forEach { page ->
            if (page.content.isNullOrEmpty()) ret += PageInfo(
                site = page.site,
                image = page.image,
                ep = page.ep,
                rawInfo = page
            )
            else {
                val pageWidth =
                    recyclerView.width - referHolder.itemView.content_container.let { it.paddingLeft + it.paddingRight }
                val titleHeight = if (page.index <= 1) referHolder.itemView.item_title.let {
                    getEpTitle(page.ep).let { content ->
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
                tempPaint.set(layout.paint)
                var spannableStringBuilder = SpannableStringBuilder()
                for (i in 0 until layout.lineCount) {
                    val lineEnd = layout.getLineEnd(i)
                    val lineStart = layout.getLineStart(i)
                    val visibleEnd = layout.getLineVisibleEnd(i)
                    spannableStringBuilder.append(SpannableString(page.content.substring(lineStart, lineEnd)).also {
                        val textCount = visibleEnd - lineStart - 1
                        if (textCount <= 1 || it.endsWith('\n') || i == layout.lineCount - 1) return@also
                        if (widthArray.size < textCount) widthArray = FloatArray(textCount)
                        val letterSpacing = (pageWidth - layout.getLineWidth(i)) / textCount / 2
                        var width = layout.getPrimaryHorizontal(lineStart + 1) + letterSpacing
                        it.setSpan(LetterSpacingSpan(width.toInt(), 0f), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        for (c in 1 until textCount) {
                            val dw = layout.getPrimaryHorizontal(lineStart + c + 1) + (2 * c + 1) * letterSpacing
                            it.setSpan(
                                LetterSpacingSpan(dw.toInt() - width.toInt(), letterSpacing + (width % 1)),
                                c,
                                c + 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            width = dw
                        }
                        it.setSpan(
                            LetterSpacingSpan(pageWidth - width.toInt(), letterSpacing + (width % 1)),
                            textCount,
                            textCount + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    })
                    val curLineBottom = layout.getLineBottom(i + 1) + titleHeight
                    if (curLineBottom - lastLineBottom < pageHeight) continue
                    ret += PageInfo(
                        content = spannableStringBuilder.subSequence(
                            0,
                            spannableStringBuilder.length - lineEnd + visibleEnd
                        ),
                        ep = page.ep,
                        rawInfo = page,
                        rawRange = IntRange(lastTextIndex, visibleEnd)
                    )
                    lastTextIndex = lineEnd
                    lastLineBottom = layout.getLineBottom(i) + titleHeight
                    spannableStringBuilder = SpannableStringBuilder()
                }
                ret += PageInfo(
                    content = spannableStringBuilder,
                    ep = page.ep,
                    rawInfo = page,
                    rawRange = IntRange(lastTextIndex, page.content.length)
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

    override fun convert(holder: BaseViewHolder, item: PageInfo) {
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
        holder.itemView.item_content.setPadding(
            0, 0, 0, if (isHorizontal) 0
            else holder.itemView.item_content.lineHeight - holder.itemView.item_content.paint.getFontMetricsInt(null)
        )
        holder.itemView.content_container.layoutParams.width = recyclerView.width
        loadData(holder, item)
    }

    @SuppressLint("SetTextI18n")
    private fun loadData(helper: BaseViewHolder, item: PageInfo) {
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
            helper.itemView.item_title.text = getEpTitle(item.ep)
            helper.itemView.item_content.text = item.content
            return
        }
        val imageRequest = requests[item.rawInfo] ?: if (item.site.isNullOrEmpty()) item.image else null
        if (imageRequest != null) {
            setImage(helper, item, imageRequest)
        } else {
            App.mainScope.launch {
                try {
                    val image =
                        (LineProvider.getProvider(Provider.TYPE_BOOK, item.site ?: "")?.provider as? BookProvider)
                            ?.getImage("${item.site}_${item.index}", item.rawInfo)
                            ?: return@launch showError(helper, "接口不存在")
                    requests[item.rawInfo] = image
                    setImage(helper, item, image)
                } catch (e: Throwable) {
                    showError(helper, "接口错误\n${e.localizedMessage}")
                }
            }
        }
    }

    private fun showError(helper: BaseViewHolder, message: String) {
        helper.itemView.loading_progress.visibility = View.GONE
        helper.itemView.loading_text.visibility = View.VISIBLE
        helper.itemView.loading_text.text = message
    }

    private fun setImage(helper: BaseViewHolder, item: PageInfo, imageRequest: Provider.HttpRequest) {
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

    private val posContent = IntArray(2)
    override fun drawSelection(c: Canvas, view: View, start: Int?, end: Int?, paint: Paint) {
        if (view.content_container.visibility != View.VISIBLE) return
        view.item_content.getLocationInWindow(posContent)
        c.save()
        c.translate(posContent[0] - recyclerView.x, posContent[1] - recyclerView.y)
        c.clipRect(0, 0, view.item_content.width, view.item_content.height)
        drawSelectionImpl(c, view, start, end, paint)
        c.restore()
    }

    val path = Path()
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
                (Math.max(
                    0f,
                    Math.min(view.item_content.width.toFloat(), layout.getPrimaryHorizontal(offset))
                ) + posContent[0] - recyclerView.x).toInt(),
                (layout.getLineTop(layout.getLineForOffset(offset)) + view.item_content.paint.getFontMetricsInt(null) + posContent[1] - recyclerView.y).toInt()
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