package soko.ekibun.bangumi.plugins.subject

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.entity.SectionEntity
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.oushangfeng.pinnedsectionitemdecoration.PinnedHeaderItemDecoration
import com.oushangfeng.pinnedsectionitemdecoration.utils.FullSpanUtil
import kotlinx.android.synthetic.main.item_episode.view.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.model.EpisodeCacheModel
import soko.ekibun.bangumi.plugins.model.cache.EpisodeCache
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class EpisodeAdapter(val linePresenter: LinePresenter, data: MutableList<EpisodeSection>? = null) :
    BaseSectionQuickAdapter<EpisodeAdapter.EpisodeSection, BaseViewHolder>
        (R.layout.item_episode_header, R.layout.item_episode, data) {

    class EpisodeSection(override val isHeader: Boolean, val header: String) : SectionEntity {
        var t: Episode? = null

        constructor(t: Episode) : this(false, "") {
            this.t = t
        }
    }

    override fun convertHeader(helper: BaseViewHolder, item: EpisodeSection) {
        //helper.getView<TextView>(R.id.item_header).visibility = if(data.indexOf(item) == 0) View.GONE else View.VISIBLE
        helper.setText(R.id.item_header, item.header)
    }

    lateinit var recyclerView: RecyclerView

    /**
     * 关联RecyclerView
     */
    fun setUpWithRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        recyclerView.adapter = this
        recyclerView.addItemDecoration(PinnedHeaderItemDecoration.Builder(SectionEntity.HEADER_TYPE).create())
    }

    override fun convert(holder: BaseViewHolder, item: EpisodeSection) {
        holder.setText(R.id.item_title, item.t!!.parseSort(holder.itemView.context))
        holder.setText(R.id.item_desc, item.t!!.displayName)
        val color = ResourceUtil.resolveColorAttr(
            holder.itemView.context,
            when (item.t!!.progress) {
                Episode.PROGRESS_WATCH -> R.attr.colorPrimary
                else -> android.R.attr.textColorSecondary
            }
        )
        val alpha = if (item.t!!.isAir) 1f else 0.6f
        holder.itemView.item_title.setTextColor(color)
        holder.itemView.item_title.alpha = alpha
        holder.itemView.item_desc.setTextColor(color)
        holder.itemView.item_desc.alpha = alpha

        holder.itemView.item_download.setOnClickListener {
            setOnItemChildClick(it, holder.layoutPosition)
        }
        holder.itemView.item_download.setOnLongClickListener {
            setOnItemChildLongClick(it, holder.layoutPosition)
        }

        MainScope().launch {
            val videoCache = EpisodeCacheModel.getEpisodeCache(item.t!!, linePresenter.subject)?.cache()
            updateDownload(holder.itemView, videoCache)
        }
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
        FullSpanUtil.onAttachedToRecyclerView(recyclerView, this, SectionEntity.HEADER_TYPE)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)
        FullSpanUtil.onViewAttachedToWindow(holder, this, SectionEntity.HEADER_TYPE)
    }
}