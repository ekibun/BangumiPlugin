package soko.ekibun.bangumi.plugins.model

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.model.cache.CacheDatabase
import soko.ekibun.bangumi.plugins.model.cache.EpisodeCache
import soko.ekibun.bangumi.plugins.model.cache.SubjectCache

object EpisodeCacheModel {
    private val cacheDao by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Room.databaseBuilder(App.app.plugin, CacheDatabase::class.java, "cache.sqlite").build().cacheDao()
    }

    fun getCacheList(): List<SubjectCache> = runBlocking {
        cacheDao.get()
    }

    fun getSubjectCacheList(subject: Subject): SubjectCache? = runBlocking {
        cacheDao.get(subject.id)
    }

    fun getEpisodeCache(episode: Episode, subject: Subject): EpisodeCache? = runBlocking {
        getSubjectCacheList(subject)?.episodeList?.firstOrNull { Episode.compareEpisode(it.episode, episode) }
    }

    fun addEpisodeCache(subject: Subject, cache: EpisodeCache) = runBlocking {
        cacheDao.inset(
            SubjectCache(
                subject,
                (getSubjectCacheList(subject)?.episodeList ?: ArrayList()).filterNot {
                    Episode.compareEpisode(
                        it.episode,
                        cache.episode
                    )
                }.plus(
                    cache
                )
            )
        )
    }

    fun removeEpisodeCache(episode: Episode, subject: Subject) = runBlocking {
        val cache = SubjectCache(
            subject,
            (getSubjectCacheList(subject)?.episodeList ?: ArrayList()).filterNot {
                Episode.compareEpisode(
                    it.episode,
                    episode
                )
            }
        )
        (if (cache.episodeList.isEmpty()) cacheDao.delete(cache) else cacheDao.inset(cache))
    }
}