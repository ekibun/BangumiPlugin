package soko.ekibun.bangumi.plugins.subject

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_provider.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.model.LineInfoModel

class SearchLineAdapter(val linePresenter: LinePresenter, data: MutableList<LineInfoModel.LineInfo>? = null) :
    BaseQuickAdapter<LineInfoModel.LineInfo, BaseViewHolder>(R.layout.item_provider, data) {
    var lines: LineInfoModel.LineInfoList? = null

    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, item: LineInfoModel.LineInfo) {
        val exist = lines?.providers?.firstOrNull { it.site == item.site && it.id == item.id } != null
        helper.itemView.item_title.alpha = if(exist) 0.7f else 1f
        helper.itemView.item_site.alpha = helper.itemView.item_title.alpha
        helper.itemView.item_id.alpha = helper.itemView.item_title.alpha

        helper.itemView.item_switch.visibility = View.GONE
        helper.itemView.item_title.text =
            (if (exist) "[已添加] " else "") + if (item.title.isEmpty()) item.id else item.title
        val provider = App.app.lineProvider.getProvider(linePresenter.type, item.site)
        helper.itemView.item_site.backgroundTintList = ColorStateList.valueOf(((provider?.color?:0) + 0xff000000).toInt())
        helper.itemView.item_site.text = provider?.title?:{ if(item.site == "") "线路" else "错误接口" }()
        helper.itemView.item_id.text = item.id
    }
}