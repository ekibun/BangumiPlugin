package soko.ekibun.bangumi.plugins.subject

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.subject_episode.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class SubjectView(private val context: Context, private val detail: View) {
    val episodeAdapter = SmallEpisodeAdapter(context)
    val episodeDetailAdapter = SmallEpisodeAdapter(context)

    init {
        episodeAdapter.emptyView = {
            val view = TextView(context)
            view.text = "点击线路加载剧集信息"
            view.gravity = Gravity.CENTER
            view.height = ResourceUtil.dip2px(context, 60f)
            view
        }()
    }

    /**
     * 更新集数标签
     */
    fun updateEpisode(subject: Subject) {
        val mainEps = subject.eps?.filter { it.type == Episode.TYPE_MAIN || it.type == Episode.TYPE_MUSIC }
        val eps = mainEps?.filter { it.isAir }
        detail.btn_detail.text =
            if (eps?.size == mainEps?.size && (subject.type == Subject.TYPE_MUSIC || subject.eps_count > 0)) context.getString(
                R.string.phrase_full_eps,
                eps?.size?:0
            ) else
                eps?.lastOrNull()?.parseSort(context)?.let { context.getString(R.string.parse_update_to, it) }
                    ?: context.getString(R.string.hint_air_nothing)

        val maps = LinkedHashMap<String, List<Episode>>()
        subject.eps?.forEach {
            val key = it.category ?: context.getString(Episode.getTypeRes(it.type))
            maps[key] = (maps[key] ?: ArrayList()).plus(it)
        }
        val lastEpisodeSize = episodeDetailAdapter.data.size
        episodeAdapter.setNewData(null)
        episodeDetailAdapter.setNewData(null)
        maps.forEach {
//            episodeDetailAdapter.addData(EpisodeAdapter.SelectableSectionEntity(true, it.key))
            it.value.forEach { ep ->
                if (ep.isAir)
                    episodeAdapter.addData(ep)
//                episodeDetailAdapter.addData(EpisodeAdapter.SelectableSectionEntity(ep))
                episodeDetailAdapter.addData(ep)
            }
        }
        if ((!scrolled || episodeDetailAdapter.data.size != lastEpisodeSize) && episodeAdapter.data.any { it.progress != null }) {
            scrolled = true

            var lastView = 0
            episodeAdapter.data.forEachIndexed { index, episode ->
                if (episode.progress in arrayOf(Episode.PROGRESS_WATCH, Episode.PROGRESS_DROP, Episode.PROGRESS_QUEUE))
                    lastView = index
            }
            val layoutManager = (detail.episode_list.layoutManager as LinearLayoutManager)
            layoutManager.scrollToPositionWithOffset(lastView, 0)
            layoutManager.stackFromEnd = false
        }
    }

    var scrolled = false
}