package soko.ekibun.bangumi.plugins.model.line

import androidx.room.Embedded
import androidx.room.Relation

data class SubjectLine(
    @Embedded var subject: SubjectLineInfo,
    @Relation(
        parentColumn = "subjectId",
        entityColumn = "subjectId"
    )
    var providers: MutableList<LineInfo>
) {
    fun getDefaultProvider(): LineInfo? {
        return providers.getOrNull(subject.defaultLine)
    }
}