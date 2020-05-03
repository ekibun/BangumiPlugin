package soko.ekibun.bangumi.plugins.model

import androidx.room.Room
import io.reactivex.schedulers.Schedulers
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.model.line.LineInfoDatabase
import soko.ekibun.bangumi.plugins.model.line.SubjectLine
import soko.ekibun.bangumi.plugins.model.line.SubjectLineInfo

object LineInfoModel {
    private val lineDao by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Room.databaseBuilder(App.app.plugin, LineInfoDatabase::class.java, "line.sqlite").build().lineDao()
    }

    fun saveInfo(subjectLine: SubjectLine) {
        subjectLine.providers.forEach {
            it.subjectId = subjectLine.subject.subjectId
        }
        lineDao.addSubjectLine(subjectLine).subscribeOn(Schedulers.io()).blockingAwait()
    }

    fun getInfo(subject: Subject): SubjectLine {
        return lineDao.getSubjectLine(subjectId = subject.id).subscribeOn(Schedulers.io()).blockingGet()
            ?: SubjectLine(SubjectLineInfo(subject.id), ArrayList())
    }
}