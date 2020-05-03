package soko.ekibun.bangumi.plugins.model.cache

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SubjectCache::class], version = 1)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}