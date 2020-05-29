package soko.ekibun.bangumi.plugins.model

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.model.line.LineInfoDatabase
import soko.ekibun.bangumi.plugins.model.line.SubjectLine

object LineInfoModel {
    private val lineDao by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Room.databaseBuilder(App.app.plugin, LineInfoDatabase::class.java, "lines.sqlite").build().lineDao()
    }

    fun saveInfo(subjectLine: SubjectLine) = runBlocking {
        lineDao.addSubjectLine(subjectLine)
    }

    fun getInfo(subject: Subject): SubjectLine = runBlocking {
        lineDao.getSubjectLine(subjectId = subject.id) ?: SubjectLine(subject.id, 0, ArrayList())
    }
}