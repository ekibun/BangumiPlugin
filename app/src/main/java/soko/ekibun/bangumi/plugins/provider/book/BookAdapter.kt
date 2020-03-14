package soko.ekibun.bangumi.plugins.provider.book

import android.annotation.SuppressLint
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_page.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.util.GlideUtil
import soko.ekibun.bangumi.plugins.util.HttpUtil

class BookAdapter(data: MutableList<BookProvider.PageInfo>? = null) :
    BaseQuickAdapter<BookProvider.PageInfo, BaseViewHolder>(R.layout.item_page, data) {
    val requests = HashMap<BookProvider.PageInfo, HttpUtil.HttpRequest>()

    override fun convert(helper: BaseViewHolder, item: BookProvider.PageInfo) {
        helper.itemView.image_sort.text = item.index.toString()
        helper.itemView.tag = item
        helper.itemView.loading_text.setOnClickListener {
            if (helper.itemView.tag == item) loadData(helper, item)
        }
        loadData(helper, item)
    }

    @SuppressLint("SetTextI18n")
    private fun loadData(helper: BaseViewHolder, item: BookProvider.PageInfo) {
        helper.itemView.item_image.visibility = View.INVISIBLE
        helper.itemView.item_content.visibility = View.GONE
        helper.itemView.item_loading.visibility = View.VISIBLE
        helper.itemView.loading_progress.visibility = View.VISIBLE
        helper.itemView.loading_text.visibility = View.GONE
        helper.itemView.loading_progress.isIndeterminate = true
        if (!item.content.isNullOrEmpty()) {
            helper.itemView.item_loading.visibility = View.GONE
            helper.itemView.item_content.visibility = View.VISIBLE
            helper.itemView.item_content.text =
                (if (item.index <= 1) "${item.ep?.category ?: ""} ${item.ep?.title}".trim() + "\n\n" else "") + item.content
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
                        helper.itemView.item_image.visibility = View.VISIBLE
                        helper.itemView.item_loading.visibility = View.GONE
                    }
                }
            }
        })
    }
}