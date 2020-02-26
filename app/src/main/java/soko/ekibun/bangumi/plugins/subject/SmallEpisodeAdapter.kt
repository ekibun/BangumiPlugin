package soko.ekibun.bangumi.plugins.subject

import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_episode_small.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.EpisodeCache
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class SmallEpisodeAdapter(val linePresenter: LinePresenter, data: MutableList<Episode>? = null) :
    BaseQuickAdapter<Episode, BaseViewHolder>(R.layout.item_episode_small, data) {

    override fun convert(helper: BaseViewHolder, item: Episode) {
        helper.itemView.item_title.text = item.parseSort(helper.itemView.context)
        helper.itemView.item_desc.text = item.displayName
        val color = ResourceUtil.resolveColorAttr(
            helper.itemView.context,
            when (item.progress) {
                Episode.PROGRESS_WATCH -> R.attr.colorPrimary
                else -> android.R.attr.textColorSecondary
            }
        )
        helper.itemView.item_title.setTextColor(color)
        helper.itemView.item_desc.setTextColor(color)
        helper.itemView.item_badge.visibility = if (item.progress in arrayOf(
                Episode.PROGRESS_WATCH,
                Episode.PROGRESS_DROP,
                Episode.PROGRESS_QUEUE
            )
        ) View.VISIBLE else View.INVISIBLE
        helper.itemView.item_badge.backgroundTintList = ColorStateList.valueOf(
            ResourceUtil.resolveColorAttr(
                helper.itemView.context,
                when (item.progress) {
                    in listOf(Episode.PROGRESS_WATCH, Episode.PROGRESS_QUEUE) -> R.attr.colorPrimary
                    else -> android.R.attr.textColorSecondary
                }
            )
        )
        helper.itemView.item_badge.text = mapOf(
            Episode.PROGRESS_WATCH to R.string.episode_status_watch,
            Episode.PROGRESS_DROP to R.string.episode_status_drop,
            Episode.PROGRESS_QUEUE to R.string.episode_status_wish
        )[item.progress ?: ""]?.let { helper.itemView.context.getString(it) } ?: ""
        helper.itemView.item_container.backgroundTintList = ColorStateList.valueOf(color)

        helper.addOnClickListener(R.id.item_container)
        helper.addOnLongClickListener(R.id.item_container)

        val videoCache = App.app.episodeCacheModel.getEpisodeCache(item, linePresenter.subject)?.cache()
        updateDownload(helper.itemView, videoCache)
    }

    fun updateDownload(view: View, cache: EpisodeCache.Cache?, downloading: Boolean = false) {
        view.item_icon.visibility = if (cache != null) View.VISIBLE else View.INVISIBLE
        view.item_icon.setImageResource(
            when {
                cache?.isFinished() ?: false -> R.drawable.ic_episode_download_ok
                downloading -> R.drawable.ic_episode_download
                else -> R.drawable.ic_episode_download_pause
            }
        )
    }
}