package soko.ekibun.bangumi.plugins.bean

import androidx.annotation.StringDef
import androidx.room.Entity
import androidx.room.Ignore

@Entity
data class Subject(
    var id: Int = 0,
    @SubjectType var type: String = TYPE_ANY,
    var name: String? = null,
    var name_cn: String? = null,
    var image: String? = null,
    @Ignore
    var eps: List<Episode>? = null,
    var eps_count: Int = 0,
    var vol_count: Int = 0,
    var ep_status: Int = 0,
    var vol_status: Int = 0
){
    val displayName get() = if (name_cn.isNullOrEmpty()) name else name_cn

    @StringDef(TYPE_ANY, TYPE_BOOK, TYPE_ANIME, TYPE_MUSIC, TYPE_GAME, TYPE_REAL)
    annotation class SubjectType

    val prefKey get() = "bgm${id}"

    companion object {
        /**
         * 条目类型定义
         */
        const val TYPE_ANY = "any"
        const val TYPE_BOOK = "book"
        const val TYPE_ANIME = "anime"
        const val TYPE_MUSIC = "music"
        const val TYPE_GAME = "game"
        const val TYPE_REAL = "real"
    }
}