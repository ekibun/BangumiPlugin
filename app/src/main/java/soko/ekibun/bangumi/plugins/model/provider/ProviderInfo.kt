package soko.ekibun.bangumi.plugins.model.provider

import androidx.room.Entity
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.util.JsonUtil

@Entity(primaryKeys = ["site", "type"])
class ProviderInfo(
    var site: String,
    var color: Int,
    var title: String,
    var type: String = "",
    var code: String = ""
) {

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