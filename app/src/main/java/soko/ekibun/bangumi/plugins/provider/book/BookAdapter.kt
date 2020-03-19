package soko.ekibun.bangumi.plugins.provider.book

import android.annotation.SuppressLint
import android.text.Layout
import android.text.StaticLayout
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_page.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.ui.view.BookLayoutManager
import soko.ekibun.bangumi.plugins.util.GlideUtil
import soko.ekibun.bangumi.plugins.util.HttpUtil
import soko.ekibun.bangumi.plugins.util.ResourceUtil

@Suppress("DEPRECATION")
class BookAdapter(data: MutableList<BookProvider.PageInfo>? = null) :
    BaseQuickAdapter<BookProvider.PageInfo, BaseViewHolder>(R.layout.item_page, data),
    BookLayoutManager.ScalableAdapter {
    val requests = HashMap<BookProvider.PageInfo, HttpUtil.HttpRequest>()

    private val referHolder by lazy { createViewHolder(recyclerView, 0) }

    val padding = ResourceUtil.dip2px(App.app.plugin, 24f)

    override fun addData(newData: MutableCollection<out BookProvider.PageInfo>) {
        super.addData(wrapData(newData.toList()))
    }

    override fun addData(position: Int, newData: MutableCollection<out BookProvider.PageInfo>) {
        super.addData(position, wrapData(newData.toList()))
    }

    private fun getEpTitle(page: BookProvider.PageInfo): String {
        return "${page.ep?.category ?: ""} ${page.ep?.title}".trim()
    }

    private fun wrapData(data: List<BookProvider.PageInfo>): List<BookProvider.PageInfo> {
        val ret = ArrayList<BookProvider.PageInfo>()
        data.forEach { page ->
            if (page.content.isNullOrEmpty()) ret += page
            else {
                val pageWidth =
                    recyclerView.width - referHolder.itemView.content_container.let { it.paddingLeft + it.paddingRight }
                val titleHeight = if (page.index <= 1) referHolder.itemView.item_title.let {
                    StaticLayout(
                        getEpTitle(page),
                        it.paint,
                        pageWidth,
                        Layout.Alignment.ALIGN_NORMAL,
                        it.lineSpacingMultiplier,
                        it.lineSpacingExtra,
                        it.includeFontPadding
                    ).height + it.paddingBottom
                } else 0
                Log.v("height", titleHeight.toString())
                val layout = referHolder.itemView.item_content.let {
                    StaticLayout(
                        page.content,
                        it.paint,
                        pageWidth,
                        Layout.Alignment.ALIGN_NORMAL,
                        it.lineSpacingMultiplier,
                        it.lineSpacingExtra,
                        it.includeFontPadding
                    )
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
                        content = page.content.substring(lastTextIndex, prevLineEndIndex).trim('\n'),
                        ep = page.ep
                    )
                    lastTextIndex = prevLineEndIndex
                    lastLineBottom = layout.getLineTop(i) + titleHeight
                }
                ret += BookProvider.PageInfo(
                    content = page.content.substring(lastTextIndex),
                    ep = page.ep
                )
            }
        }
        ret.forEachIndexed { index, page ->
            page.index = index + 1
        }
        return ret
    }

    override fun convert(helper: BaseViewHolder, item: BookProvider.PageInfo) {
        helper.itemView.image_sort.text = item.index.toString()
        helper.itemView.tag = item
        helper.itemView.loading_text.setOnClickListener {
            if (helper.itemView.tag == item) loadData(helper, item)
        }
        helper.itemView.content_container.layoutParams.width = recyclerView.width
        loadData(helper, item)
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
                ?.getImage("${item.site}_${item.index}", App.app.jsEngine, item)?.enqueue({
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
        }, { type, _ ->
            if (helper.itemView.tag == item) {
                when (type) {
                    GlideUtil.TYPE_ERROR -> {
                        showError(helper, "加载出错")
                    }
                    GlideUtil.TYPE_RESOURCE -> {
                        helper.itemView.item_loading.visibility = View.GONE
                    }
                }
            }
        })
    }

    override fun isItemScalable(pos: Int, layoutManager: LinearLayoutManager): Boolean {
        return layoutManager.findViewByPosition(pos)?.content_container?.visibility != View.VISIBLE
    }
}