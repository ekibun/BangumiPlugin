package soko.ekibun.bangumi.plugins.model

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.util.JsonUtil

class ProgressModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    private fun prefKey(subject: Subject): String{
        return "${PREF_PROGRESS_INFO}_${subject.prefKey}"
    }

    fun saveProgress(subject: Subject, info: Info) {
        val editor = sp.edit()
        val key = prefKey(subject)
        editor.putString(key, JsonUtil.toJson(info))
        editor.apply()
    }

    fun getProgress(subject: Subject): Info? {
        return JsonUtil.toEntity<Info>(sp.getString(prefKey(subject), "")!!)
    }

    data class Info(
        val episode: Episode,
        val progress: Int)

    companion object {
        const val PREF_PROGRESS_INFO="progressInfo"
    }
}