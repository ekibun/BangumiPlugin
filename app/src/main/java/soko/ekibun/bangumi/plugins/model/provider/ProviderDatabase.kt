package soko.ekibun.bangumi.plugins.model.provider

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProviderInfo::class], version = 1)
abstract class ProviderDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
}