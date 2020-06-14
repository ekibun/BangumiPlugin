package soko.ekibun.bangumi.plugins.subject

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.subject_episode.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.Subject
import java.text.DecimalFormat

class SubjectView(private val linePresenter: LinePresenter, private val detail: View) {
    val episodeAdapter = SmallEpisodeAdapter(linePresenter)
    val episodeDetailAdapter = EpisodeAdapter(linePresenter)

    /**
     * 更新集数标签
     */
    fun updateEpisode(subject: Subject) {
        val mainEps = subject.eps?.filter { it.type == Episode.TYPE_MAIN || it.type == Episode.TYPE_MUSIC }
        val eps = mainEps?.filter { it.isAir }
        detail.btn_detail.text =
            if (eps?.size == mainEps?.size && (subject.type == Subject.TYPE_MUSIC || subject.eps_count > 0)) linePresenter.pluginContext.getString(
                R.string.phrase_full_eps,
                eps?.size?:0
            ) else
                eps?.lastOrNull()?.let {
                    linePresenter.pluginContext.getString(
                        R.string.parse_update_to,
                        linePresenter.pluginContext.getString(
                            R.string.parse_sort_ep,
                            DecimalFormat("#.##").format(it.sort)
                        )
                    )
                }
                    ?: linePresenter.pluginContext.getString(R.string.hint_air_nothing)

        val maps = LinkedHashMap<String, List<Episode>>()
        subject.eps?.forEach {
            val key = it.category ?: linePresenter.pluginContext.getString(Episode.getTypeRes(it.type))
            maps[key] = (maps[key] ?: ArrayList()).plus(it)
        }
        val lastEpisodeSize = episodeDetailAdapter.data.size
        episodeAdapter.setNewInstance(null)
        episodeDetailAdapter.setNewInstance(null)
        maps.forEach {
            episodeDetailAdapter.addData(EpisodeAdapter.EpisodeSection(true, it.key))
            it.value.forEach { ep ->
                if (ep.isAir)
                    episodeAdapter.addData(ep)
                episodeDetailAdapter.addData(EpisodeAdapter.EpisodeSection(ep))
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