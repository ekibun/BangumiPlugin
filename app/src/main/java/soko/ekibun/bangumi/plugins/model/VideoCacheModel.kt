package soko.ekibun.bangumi.plugins.model

import android.content.Context
import androidx.preference.PreferenceManager
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.bean.SubjectCache
import soko.ekibun.bangumi.plugins.bean.VideoCache
import soko.ekibun.bangumi.plugins.util.JsonUtil
import kotlin.collections.HashMap

class VideoCacheModel(context: Context) {
    private val sp by lazy { PreferenceManager.getDefaultSharedPreferences(context)!! }
    private val cacheList by lazy {
        JsonUtil.toEntity<HashMap<String, SubjectCache>>(
            sp.getString(
                PREF_VIDEO_CACHE,
                JsonUtil.toJson(HashMap<String, SubjectCache>())
            )!!
        ) ?: HashMap()
    }

    fun getCacheList(site: String): List<SubjectCache> {
        return cacheList.filter { it.key.startsWith(site) }.values.toList()
    }

    fun getSubjectCacheList(subject: Subject): SubjectCache? {
        return cacheList[subject.prefKey]
    }

    fun getVideoCache(episode: Episode, subject: Subject): VideoCache? {
        return getSubjectCacheList(subject)?.videoList?.firstOrNull { it.episode.id == episode.id }
    }

    fun addVideoCache(subject: Subject, cache: VideoCache) {
        val editor = sp.edit()
        cacheList[subject.prefKey] = SubjectCache(
            subject,
            (cacheList[subject.prefKey]?.videoList ?: ArrayList()).filterNot { it.episode.id == cache.episode.id }.plus(
                cache
            )
        )
        editor.putString(PREF_VIDEO_CACHE, JsonUtil.toJson(cacheList))
        editor.apply()
    }

    fun removeVideoCache(episode: Episode, subject: Subject) {
        val editor = sp.edit()
        cacheList[subject.prefKey] = SubjectCache(
            subject,
            (cacheList[subject.prefKey]?.videoList ?: ArrayList()).filterNot { it.episode.id == episode.id })
        cacheList[subject.prefKey]?.let {
            if (it.videoList.isEmpty()) cacheList.remove(subject.prefKey)
        }
        editor.putString(PREF_VIDEO_CACHE, JsonUtil.toJson(cacheList))
        editor.apply()
    }

    companion object {
        const val PREF_VIDEO_CACHE = "videoCache"

        fun isFinished(downloadPercentage: Float): Boolean {
            return Math.abs(downloadPercentage - 100f) < 0.001f
        }
    }
}