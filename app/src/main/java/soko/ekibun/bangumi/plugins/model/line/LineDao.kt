package soko.ekibun.bangumi.plugins.model.line

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface LineDao {
    @Query("SELECT * FROM SubjectLine WHERE subjectId = :subjectId")
    fun getSubjectLine(subjectId: Int): Maybe<SubjectLine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSubjectLine(subjectLine: SubjectLine): Completable
}