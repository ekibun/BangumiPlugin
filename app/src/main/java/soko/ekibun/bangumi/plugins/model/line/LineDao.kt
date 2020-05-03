package soko.ekibun.bangumi.plugins.model.line

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
abstract class LineDao {
    @Transaction
    @Query("SELECT * FROM SubjectLineInfo WHERE subjectId = :subjectId")
    abstract fun getSubjectLine(subjectId: Int): Maybe<SubjectLine>

    @Query("DELETE FROM LineInfo WHERE subjectId = :subjectId")
    abstract fun deleteLineInfo(subjectId: Int): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addLineInfo(infoList: List<LineInfo>): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addSubjectLineInfo(subjectInfo: SubjectLineInfo): Completable

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSubjectLine(subjectLine: SubjectLine): Completable {
        return addSubjectLineInfo(subjectLine.subject)
            .concatWith(deleteLineInfo(subjectLine.subject.subjectId))
            .concatWith(addLineInfo(subjectLine.providers))
    }
}