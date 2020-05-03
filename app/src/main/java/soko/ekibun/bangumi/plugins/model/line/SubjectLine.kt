package soko.ekibun.bangumi.plugins.model.line

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity
@TypeConverters(LineInfo.EntityConverter::class)
data class SubjectLine(
    @PrimaryKey var subjectId: Int,
    var defaultLine: Int = 0,
    var providers: ArrayList<LineInfo>
) {
    fun getDefaultProvider(): LineInfo? {
        return providers.getOrNull(defaultLine)
    }
}