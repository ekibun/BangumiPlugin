package soko.ekibun.bangumi.plugins.model.line

import androidx.room.TypeConverter
import soko.ekibun.bangumi.plugins.util.JsonUtil

data class LineInfo(
    var site: String,
    var id: String,
    var title: String = "",
    var extra: String? = null
) {
    class EntityConverter {
        @TypeConverter
        fun getFromString(value: String): ArrayList<LineInfo>? {
            return JsonUtil.toEntity(value)
        }

        @TypeConverter
        fun storeToString(list: ArrayList<LineInfo>): String {
            return JsonUtil.toJson(list)
        }
    }
}