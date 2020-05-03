package soko.ekibun.bangumi.plugins.main

import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.model.cache.SubjectCache
import soko.ekibun.bangumi.plugins.util.GlideUtil
import soko.ekibun.bangumi.plugins.util.ResourceUtil

/**
 * 缓存列表Adapter
 */
class CacheAdapter(data: MutableList<SubjectCache>? = null) :
    BaseQuickAdapter<SubjectCache, BaseViewHolder>
        (R.layout.item_subject, data) {
    override fun convert(holder: BaseViewHolder, item: SubjectCache) {
        holder.setText(R.id.item_title, item.subject.displayName)
        holder.setText(
            R.id.item_name_jp,
            holder.itemView.context.getString(R.string.parse_cache_eps, item.episodeList.size)
        )
        GlideUtil.with(App.app.host)
            ?.load(item.subject.image)
            ?.apply(RequestOptions.errorOf(ResourceUtil.getResId(App.app.host, "drawable", "ic_404")))
            ?.into(holder.itemView.item_cover)
    }
}