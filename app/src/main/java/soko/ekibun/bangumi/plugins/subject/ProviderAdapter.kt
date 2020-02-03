package soko.ekibun.bangumi.plugins.subject

import android.content.Context
import com.zhy.adapter.abslistview.CommonAdapter
import com.zhy.adapter.abslistview.ViewHolder
import soko.ekibun.bangumi.plugins.model.LineProvider

class ProviderAdapter(context: Context?, data: List<LineProvider.ProviderInfo>?) :
    CommonAdapter<LineProvider.ProviderInfo>(context, android.R.layout.simple_spinner_dropdown_item, data) {
    override fun convert(viewHolder: ViewHolder, item: LineProvider.ProviderInfo, position: Int) {
        viewHolder.setText(android.R.id.text1, item.title)
    }

}