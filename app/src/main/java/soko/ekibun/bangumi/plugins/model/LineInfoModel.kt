package soko.ekibun.bangumi.plugins.model

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.util.JsonUtil

class LineInfoModel(context: Context){
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
            return providers.getOrNull(Math.min(providers.size - 1, defaultProvider))
        }
    }

    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    private fun prefKey(subject: Subject): String {
        return "${PREF_LINE_INFO_LIST}_${subject.prefKey}"
    }

    fun saveInfos(subject: Subject, infos: LineInfoList) {
        val editor = sp.edit()
        val key = prefKey(subject)
        if(infos.providers.size == 0){
            editor.remove(key)
        }else{
            editor.putString(key, JsonUtil.toJson(infos))
        }
        editor.apply()
    }

    fun getInfos(subject: Subject): LineInfoList? {
        return JsonUtil.toEntity<LineInfoList>(sp.getString(prefKey(subject), JsonUtil.toJson(LineInfoList()))!!)
    }

    companion object{
        const val PREF_LINE_INFO_LIST="lineInfoList"
    }
}