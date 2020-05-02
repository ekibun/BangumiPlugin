package soko.ekibun.bangumi.plugins.provider.book

import android.annotation.SuppressLint
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
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.ui.view.BookLayoutManager
import soko.ekibun.bangumi.plugins.util.GlideUtil
import soko.ekibun.bangumi.plugins.util.HttpUtil
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class BookAdapter(val recyclerView: RecyclerView, data: MutableList<BookProvider.PageInfo>? = null) :
    BaseQuickAdapter<BookProvider.PageInfo, BaseViewHolder>(R.layout.item_page, data),
    BookLayoutManager.ScalableAdapter {
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
                for (i in 0 until layout.lineCount) {
                    val curLineBottom = layout.getLineBottom(i) + titleHeight
                    if (curLineBottom - lastLineBottom < pageHeight) continue
                    val prevLineEndIndex = layout.getLineStart(i)
                    ret += BookProvider.PageInfo(
                        content = page.content.substring(lastTextIndex, prevLineEndIndex),
                        ep = page.ep,
                        rawInfo = page,
                        rawRange = Pair(lastTextIndex, prevLineEndIndex)
                    )
                    lastTextIndex = prevLineEndIndex
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
            (App.app.lineProvider.getProvider(Provider.TYPE_BOOK, item.site ?: "")?.provider as? BookProvider)
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
}