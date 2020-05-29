package soko.ekibun.bangumi.plugins.model.provider

import androidx.room.*

@Dao
interface ProviderDao {
    @Query("SELECT * FROM ProviderInfo WHERE type = :type")
    suspend fun get(type: String): List<ProviderInfo>

    @Query("SELECT * FROM ProviderInfo WHERE site = :site AND type = :type")
    suspend fun get(type: String, site: String): ProviderInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provider: ProviderInfo)

    @Delete
    suspend fun delete(provider: ProviderInfo)

}