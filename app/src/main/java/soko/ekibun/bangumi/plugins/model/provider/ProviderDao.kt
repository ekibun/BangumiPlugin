package soko.ekibun.bangumi.plugins.model.provider

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface ProviderDao {
    @Query("SELECT * FROM ProviderInfo WHERE type = :type")
    fun get(type: String): Single<List<ProviderInfo>>

    @Query("SELECT * FROM ProviderInfo WHERE site = :site AND type = :type")
    fun get(type: String, site: String): Maybe<ProviderInfo>

    @Insert
    fun insert(provider: ProviderInfo): Completable

    @Delete
    fun delete(provider: ProviderInfo): Completable

}