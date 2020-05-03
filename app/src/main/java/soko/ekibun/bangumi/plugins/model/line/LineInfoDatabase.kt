package soko.ekibun.bangumi.plugins.model.line

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LineInfo::class, SubjectLineInfo::class], version = 1)
abstract class LineInfoDatabase : RoomDatabase() {
    abstract fun lineDao(): LineDao
}