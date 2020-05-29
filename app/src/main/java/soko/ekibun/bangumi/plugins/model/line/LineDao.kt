package soko.ekibun.bangumi.plugins.model.line

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LineDao {
    @Query("SELECT * FROM SubjectLine WHERE subjectId = :subjectId")
    suspend fun getSubjectLine(subjectId: Int): SubjectLine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSubjectLine(subjectLine: SubjectLine)
}