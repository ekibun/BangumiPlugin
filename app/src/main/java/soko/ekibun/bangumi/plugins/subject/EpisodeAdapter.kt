package soko.ekibun.bangumi.plugins.subject

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.SectionEntity
import com.oushangfeng.pinnedsectionitemdecoration.PinnedHeaderItemDecoration
import com.oushangfeng.pinnedsectionitemdecoration.utils.FullSpanUtil
import kotlinx.android.synthetic.main.item_episode.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.EpisodeCache
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class EpisodeAdapter(val linePresenter: LinePresenter, data: MutableList<EpisodeSection>? = null) :
    BaseSectionQuickAdapter<EpisodeAdapter.EpisodeSection, BaseViewHolder>
        (R.layout.item_episode, R.layout.item_episode_header, data) {

    class EpisodeSection(isHeader: Boolean, header: String) : SectionEntity<Episode>(isHeader, header) {
        constructor(t: Episode) : this(false, "") {
            this.t = t
        }
    }

    override fun convertHead(helper: BaseViewHolder, item: EpisodeSection) {
        //helper.getView<TextView>(R.id.item_header).visibility = if(data.indexOf(item) == 0) View.GONE else View.VISIBLE
        helper.setText(R.id.item_header, item.header)
    }

    /**
     * 关联RecyclerView
     */
    fun setUpWithRecyclerView(recyclerView: RecyclerView) {
        bindToRecyclerView(recyclerView)
        recyclerView.addItemDecoration(PinnedHeaderItemDecoration.Builder(SECTION_HEADER_VIEW).create())
    }

    override fun convert(helper: BaseViewHolder, item: EpisodeSection) {
        helper.setText(R.id.item_title, item.t.parseSort(helper.itemView.context))
        helper.setText(R.id.item_desc, item.t.displayName)
        val color = ResourceUtil.resolveColorAttr(
            helper.itemView.context,
            when (item.t.progress) {
                Episode.PROGRESS_WATCH -> R.attr.colorPrimary
                else -> android.R.attr.textColorSecondary
            }
        )
        val alpha = if (item.t.isAir) 1f else 0.6f
        helper.itemView.item_title.setTextColor(color)
        helper.itemView.item_title.alpha = alpha
        helper.itemView.item_desc.setTextColor(color)
        helper.itemView.item_desc.alpha = alpha

        helper.addOnClickListener(R.id.item_download)
        helper.addOnLongClickListener(R.id.item_download)

        val videoCache = App.app.episodeCacheModel.getEpisodeCache(item.t, linePresenter.subject)?.cache()
        updateDownload(helper.itemView, videoCache)
    }

    fun updateDownload(itemView: View, cache: EpisodeCache.Cache?, download: Boolean = false) {
        itemView.item_download_info.text = cache?.getProgressInfo() ?: ""
        if (cache != null && !cache.isFinished()) {
            itemView.item_progress.max = 10000
            itemView.item_progress.progress = (cache.getProgress() * 10000).toInt()
            itemView.item_progress.isEnabled = download
            itemView.item_progress.visibility = View.VISIBLE
        } else {
            itemView.item_progress.visibility = View.INVISIBLE
        }
        itemView.item_download.setImageResource(
            if (cache?.isFinished() == true) R.drawable.ic_cloud_done else if (download) R.drawable.ic_pause else R.drawable.ic_download
        )
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        FullSpanUtil.onAttachedToRecyclerView(recyclerView, this, SECTION_HEADER_VIEW)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)
        FullSpanUtil.onViewAttachedToWindow(holder, this, SECTION_HEADER_VIEW)
    }
}