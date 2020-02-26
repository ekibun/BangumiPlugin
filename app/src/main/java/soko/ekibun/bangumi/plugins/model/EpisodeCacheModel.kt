package soko.ekibun.bangumi.plugins.model

import android.content.Context
import androidx.preference.PreferenceManager
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.EpisodeCache
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.bean.SubjectCache
import soko.ekibun.bangumi.plugins.util.JsonUtil

class EpisodeCacheModel(context: Context) {
    private val sp by lazy { PreferenceManager.getDefaultSharedPreferences(context)!! }
    private val cacheList by lazy {
        JsonUtil.toEntity<HashMap<String, SubjectCache>>(
            sp.getString(
                PREF_EPISODE_CACHE,
                JsonUtil.toJson(HashMap<String, SubjectCache>())
            )!!
        ) ?: HashMap()
    }

    fun getCacheList(): List<SubjectCache> {
        return cacheList.values.toList()
    }

    fun getSubjectCacheList(subject: Subject): SubjectCache? {
        return cacheList[subject.prefKey]
    }

    fun getEpisodeCache(episode: Episode, subject: Subject): EpisodeCache? {
        return getSubjectCacheList(subject)?.episodeList?.firstOrNull { Episode.compareEpisode(it.episode, episode) }
    }

    fun addEpisodeCache(subject: Subject, cache: EpisodeCache) {
        val editor = sp.edit()
        cacheList[subject.prefKey] = SubjectCache(
            subject,
            (cacheList[subject.prefKey]?.episodeList ?: ArrayList()).filterNot {
                Episode.compareEpisode(
                    it.episode,
                    cache.episode
                )
            }.plus(
                cache
            )
        )
        editor.putString(PREF_EPISODE_CACHE, JsonUtil.toJson(cacheList))
        editor.apply()
    }

    fun removeEpisodeCache(episode: Episode, subject: Subject) {
        val editor = sp.edit()
        cacheList[subject.prefKey] = SubjectCache(
            subject,
            (cacheList[subject.prefKey]?.episodeList ?: ArrayList()).filterNot {
                Episode.compareEpisode(
                    it.episode,
                    episode
                )
            })
        cacheList[subject.prefKey]?.let {
            if (it.episodeList.isEmpty()) cacheList.remove(subject.prefKey)
        }
        editor.putString(PREF_EPISODE_CACHE, JsonUtil.toJson(cacheList))
        editor.apply()
    }

    companion object {
        const val PREF_EPISODE_CACHE = "episodeCache"
    }
}