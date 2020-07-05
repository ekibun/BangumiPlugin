package soko.ekibun.bangumi.plugins.model.provider

import android.util.Base64
import androidx.room.Entity
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.util.GzipUtil
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

    companion object {
        private const val BASE64_FLAG = Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
        fun toUrl(data: List<ProviderInfo>): String {
            return "ekibun://bangumi/plugin@" + Base64.encodeToString(
                GzipUtil.compress(JsonUtil.toJson(data)), BASE64_FLAG
            )
        }

        fun fromUrl(src: String): List<ProviderInfo>? {
            return try {
                JsonUtil.toEntity<List<ProviderInfo>>(
                    GzipUtil.uncompressToString(
                        Base64.decode(
                            src.substringAfter('@'),
                            BASE64_FLAG
                        )
                    ) ?: return null
                )
            } catch (e: Throwable) {
                null
            }
        }
    }
}