package soko.ekibun.bangumi.plugins.provider.video

import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_provider.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider

class DanmakuListAdapter(data: MutableList<DanmakuInfo>? = null) :
    BaseQuickAdapter<DanmakuListAdapter.DanmakuInfo, BaseViewHolder>(R.layout.item_provider, data) {

    override fun convert(helper: BaseViewHolder, item: DanmakuInfo) {
        helper.itemView.item_title.text = item.line.id
        val providerInfo = LineProvider.getProvider(Provider.TYPE_VIDEO, item.line.site)?:return
        helper.itemView.item_switch.visibility = View.GONE
        helper.itemView.item_site.backgroundTintList = ColorStateList.valueOf((0xff000000 + providerInfo.color).toInt())
        helper.itemView.item_site.text = providerInfo.title
        helper.itemView.item_id.text = if(item.info.isNotEmpty()) item.info else " ${item.danmakus.size} 条弹幕"
    }

    data class DanmakuInfo(
        val line: LineInfoModel.LineInfo,
        var danmakus: HashSet<VideoProvider.DanmakuInfo> = HashSet(),
        var info: String = "",
        var videoInfo: VideoProvider.VideoInfo? = null,
        var key: String? = null
    )
}