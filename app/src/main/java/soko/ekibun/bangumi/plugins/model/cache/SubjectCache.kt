package soko.ekibun.bangumi.plugins.model.cache

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.TypeConverters
import soko.ekibun.bangumi.plugins.bean.Subject

@Entity(primaryKeys = ["id"])
@TypeConverters(EpisodeCache.EntityConverter::class)
data class SubjectCache(
    @Embedded val subject: Subject,
    val episodeList: List<EpisodeCache>
)