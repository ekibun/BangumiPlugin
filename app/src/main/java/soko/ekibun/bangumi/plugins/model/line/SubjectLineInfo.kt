package soko.ekibun.bangumi.plugins.model.line

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SubjectLineInfo(
    @PrimaryKey var subjectId: Int,
    var defaultLine: Int = 0
)