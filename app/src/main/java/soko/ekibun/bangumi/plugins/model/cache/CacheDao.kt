package soko.ekibun.bangumi.plugins.model.cache

import androidx.room.*

@Dao
interface CacheDao {
    @Query("SELECT * FROM SubjectCache")
    suspend fun get(): List<SubjectCache>

    @Query("SELECT * FROM SubjectCache WHERE id = :id")
    suspend fun get(id: Int): SubjectCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inset(subjectCache: SubjectCache)

    @Delete
    suspend fun delete(subjectCache: SubjectCache)
}