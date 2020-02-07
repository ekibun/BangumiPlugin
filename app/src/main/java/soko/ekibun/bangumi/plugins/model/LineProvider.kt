package soko.ekibun.bangumi.plugins.model

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.util.JsonUtil

class LineProvider(context: Context) {

    class ProviderInfo(
        var site: String,
        var color: Int,
        var title: String,
        var type: String = "",
        var code: String = ""
    ){
        val prefKey get() = "${type}_${site}"

        val provider get() = Provider.providers[type]?.let { JsonUtil.toEntity(code, it) }

        override fun hashCode(): Int {
            return site.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (site != (other as ProviderInfo).site) return false
            return true
        }
    }

    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    val providerList by lazy { JsonUtil.toEntity<HashMap<String, ProviderInfo>>(sp.getString(
        PREF_PROVIDER, JsonUtil.toJson(HashMap<String, ProviderInfo>()))!!)?: HashMap() }

    fun getProvider(type: String, site: String): ProviderInfo? {
        return providerList["${type}_${site}"]
    }
    fun addProvider(provider: ProviderInfo){
        val editor = sp.edit()
        providerList[provider.prefKey] = provider
        editor.putString(PREF_PROVIDER, JsonUtil.toJson(providerList))
        editor.apply()
    }
    fun removeProvider(provider: ProviderInfo){
        val editor = sp.edit()
        providerList.remove(provider.prefKey)
        editor.putString(PREF_PROVIDER, JsonUtil.toJson(providerList))
        editor.apply()
    }

    companion object{
        const val PREF_PROVIDER="mangaProvider"
    }
}