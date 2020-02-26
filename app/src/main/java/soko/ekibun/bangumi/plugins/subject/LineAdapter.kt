package soko.ekibun.bangumi.plugins.subject

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import com.zhy.adapter.abslistview.CommonAdapter
import com.zhy.adapter.abslistview.ViewHolder
import kotlinx.android.synthetic.main.item_provider.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class LineAdapter(val type: String, context: Context?, data: List<LineInfoModel.LineInfo>?) :
    CommonAdapter<LineInfoModel.LineInfo>(context, R.layout.item_provider, data) {
    var selectIndex = 0


    override fun convert(viewHolder: ViewHolder, item: LineInfoModel.LineInfo, position: Int) {
        val color = ResourceUtil.resolveColorAttr(
            viewHolder.convertView.context,
            if (position == selectIndex) R.attr.colorPrimary else android.R.attr.textColorSecondary
        )
        viewHolder.convertView.item_title.setTextColor(color)
        viewHolder.convertView.item_title.text = if (item.title.isEmpty()) item.id else item.title
        val provider = App.app.lineProvider.getProvider(type, item.site)
        viewHolder.convertView.item_switch.visibility = View.GONE
        if (position < count - 2) {
            viewHolder.convertView.item_site.visibility = View.VISIBLE
            viewHolder.convertView.item_id.visibility = View.VISIBLE
            viewHolder.convertView.item_site.backgroundTintList =
                ColorStateList.valueOf(((provider?.color ?: 0) + 0xff000000).toInt())
            viewHolder.convertView.item_site.text = provider?.title ?: { if (item.site == "") "线路" else "错误接口" }()
            viewHolder.convertView.item_id.text = item.id
        } else {
            viewHolder.convertView.item_site.visibility = View.INVISIBLE
            viewHolder.convertView.item_id.visibility = View.GONE
        }

    }

}