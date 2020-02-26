package soko.ekibun.bangumi.plugins.bean

data class SubjectCache(
    val subject: Subject,
    val episodeList: List<EpisodeCache>
)