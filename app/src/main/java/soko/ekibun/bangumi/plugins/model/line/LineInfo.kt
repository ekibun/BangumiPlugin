package soko.ekibun.bangumi.plugins.model.line

import androidx.room.Entity

@Entity(primaryKeys = ["site", "id", "subjectId"])
data class LineInfo(
    var site: String,
    var id: String,
    var title: String = "",
    var extra: String? = null,
    var subjectId: Int
)