package soko.ekibun.bangumi.plugins.model.cache

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface CacheDao {
    @Query("SELECT * FROM SubjectCache")
    fun get(): Single<List<SubjectCache>>

    @Query("SELECT * FROM SubjectCache WHERE id = :id")
    fun get(id: Int): Maybe<SubjectCache>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun inset(subjectCache: SubjectCache): Completable

    @Delete
    fun delete(subjectCache: SubjectCache): Completable
}