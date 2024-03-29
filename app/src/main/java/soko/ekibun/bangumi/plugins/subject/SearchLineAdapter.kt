package soko.ekibun.bangumi.plugins.subject

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_provider.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.line.SubjectLine

class SearchLineAdapter(val linePresenter: LinePresenter, data: MutableList<LineInfo>? = null) :
    BaseQuickAdapter<LineInfo, BaseViewHolder>(R.layout.item_provider, data) {
    var lines: SubjectLine? = null

    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: LineInfo) {
        val exist = lines?.providers?.firstOrNull { it.site == item.site && it.id == item.id } != null
        holder.itemView.item_title.alpha = if (exist) 0.7f else 1f
        holder.itemView.item_site.alpha = holder.itemView.item_title.alpha
        holder.itemView.item_id.alpha = holder.itemView.item_title.alpha

        holder.itemView.item_switch.visibility = View.GONE
        holder.itemView.item_title.text =
            (if (exist) "[已添加] " else "") + if (item.title.isEmpty()) item.id else item.title
        val provider = LineProvider.getProvider(linePresenter.type, item.site)
        holder.itemView.item_site.backgroundTintList =
            ColorStateList.valueOf(((provider?.color ?: 0) + 0xff000000).toInt())
        holder.itemView.item_site.text = provider?.title ?: { if (item.site == "") "线路" else "错误接口" }()
        holder.itemView.item_id.text = item.id
    }
}