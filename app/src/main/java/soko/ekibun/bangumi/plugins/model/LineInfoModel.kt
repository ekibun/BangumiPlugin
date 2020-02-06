package soko.ekibun.bangumi.plugins.model

import com.pl.sphelper.SPHelper
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.util.JsonUtil

object LineInfoModel {
    data class LineInfo(
        var site: String,
        var id: String,
        var title: String = "",
        var extra: String = ""
    )

    data class LineInfoList(
        val providers: ArrayList<LineInfo> = ArrayList(),
        var defaultProvider: Int = 0
    ) {
        fun getDefaultProvider(): LineInfo? {
            return providers.getOrNull(defaultProvider)
        }
    }

    private fun prefKey(subject: Subject): String {
        return "${PREF_LINE_INFO_LIST}_${subject.prefKey}"
    }

    fun saveInfos(subject: Subject, infos: LineInfoList) {
        SPHelper.save(prefKey(subject), JsonUtil.toJson(infos))
    }

    fun getInfos(subject: Subject): LineInfoList? {
        return JsonUtil.toEntity<LineInfoList>(SPHelper.getString(prefKey(subject), JsonUtil.toJson(LineInfoList()))!!)
    }

    const val PREF_LINE_INFO_LIST = "lineInfoList"
}