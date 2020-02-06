package soko.ekibun.bangumi.plugins.model

import com.pl.sphelper.SPHelper
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.util.JsonUtil

object LineProvider {

    class ProviderInfo(
        var site: String,
        var color: Int,
        var title: String,
        var type: String = "",
        var code: String = ""
    ) {
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

    val providerList = {
        JsonUtil.toEntity<HashMap<String, ProviderInfo>>(
            SPHelper.getString(PREF_PROVIDER, JsonUtil.toJson(HashMap<String, ProviderInfo>()))!!
        ) ?: HashMap()
    }

    fun getProvider(type: String, site: String): ProviderInfo? {
        return providerList()["${type}_${site}"]
    }

    fun addProvider(provider: ProviderInfo) {
        val providerList = providerList()
        providerList[provider.prefKey] = provider
        SPHelper.save(PREF_PROVIDER, JsonUtil.toJson(providerList))
    }

    fun removeProvider(provider: ProviderInfo) {
        val providerList = providerList()
        providerList.remove(provider.prefKey)
        SPHelper.save(PREF_PROVIDER, JsonUtil.toJson(providerList))
    }

    const val PREF_PROVIDER = "mangaProvider"
}