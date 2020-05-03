package soko.ekibun.bangumi.plugins.model

import androidx.room.Room
import io.reactivex.schedulers.Schedulers
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

    fun getCacheList(): List<SubjectCache> {
        return cacheDao.get().subscribeOn(Schedulers.io()).blockingGet()
    }

    fun getSubjectCacheList(subject: Subject): SubjectCache? {
        return cacheDao.get(subject.id).subscribeOn(Schedulers.io()).blockingGet()
    }

    fun getEpisodeCache(episode: Episode, subject: Subject): EpisodeCache? {
        return getSubjectCacheList(subject)?.episodeList?.firstOrNull { Episode.compareEpisode(it.episode, episode) }
    }

    fun addEpisodeCache(subject: Subject, cache: EpisodeCache) {
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
        ).subscribeOn(Schedulers.io()).subscribe()
    }

    fun removeEpisodeCache(episode: Episode, subject: Subject) {
        cacheDao.inset(SubjectCache(
            subject,
            (getSubjectCacheList(subject)?.episodeList ?: ArrayList()).filterNot {
                Episode.compareEpisode(
                    it.episode,
                    episode
                )
            }
        )).subscribeOn(Schedulers.io()).subscribe()
    }
}