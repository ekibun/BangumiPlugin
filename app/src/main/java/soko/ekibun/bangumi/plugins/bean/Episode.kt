package soko.ekibun.bangumi.plugins.bean

import android.content.Context
import androidx.annotation.IntDef
import androidx.annotation.StringDef
import androidx.annotation.StringRes
import soko.ekibun.bangumi.plugins.R
import java.text.DecimalFormat

data class Episode(
    val id: Int = 0,
    @EpisodeType var type: Int = 0,
    var sort: Float = 0f,
    var name: String? = null,
    var name_cn: String? = null,
    @EpisodeStatus var status: String? = null,
    @ProgressType var progress: String? = null,
    var category: String? = null
){
    val displayName get() = if (name_cn.isNullOrEmpty()) name else name_cn
    val isAir get() = status == STATUS_AIR || progress == PROGRESS_WATCH || (category?.startsWith("Disc") ?: false)

    /**
     * 第*话
     */
    fun parseSort(context: Context): String {
        return if (type == TYPE_MAIN)
            context.getString(R.string.parse_sort_ep, DecimalFormat("#.##").format(sort))
        else
            (category ?: context.getString(getTypeRes(type))) + " ${DecimalFormat("#.##").format(sort)}"
    }

    /**
     * 剧集类型
     */
    @IntDef(TYPE_MAIN, TYPE_SP, TYPE_OP, TYPE_ED, TYPE_PV, TYPE_MAD, TYPE_OTHER, TYPE_MUSIC)
    annotation class EpisodeType

    /**
     * 剧集状态
     */
    @StringDef(STATUS_TODAY, STATUS_AIR, STATUS_NA)
    annotation class EpisodeStatus

    /**
     * 进度状态
     */
    @StringDef(PROGRESS_WATCH, PROGRESS_QUEUE, PROGRESS_DROP, PROGRESS_REMOVE)
    annotation class ProgressType

    companion object {
        const val TYPE_MAIN = 0
        const val TYPE_SP = 1
        const val TYPE_OP = 2
        const val TYPE_ED = 3
        const val TYPE_PV = 4
        const val TYPE_MAD = 5
        const val TYPE_OTHER = 6
        const val TYPE_MUSIC = 7

        const val STATUS_TODAY = "Today"
        const val STATUS_AIR = "Air"
        const val STATUS_NA = "NA"

        const val PROGRESS_WATCH = "watched"
        const val PROGRESS_QUEUE = "queue"
        const val PROGRESS_DROP = "drop"
        const val PROGRESS_REMOVE = "remove"

        /**
         * 剧集类型字符串资源
         */
        @StringRes
        fun getTypeRes(@EpisodeType type: Int): Int {
            return when (type) {
                TYPE_MAIN -> R.string.episode_type_main
                TYPE_SP -> R.string.episode_type_sp
                TYPE_OP -> R.string.episode_type_op
                TYPE_ED -> R.string.episode_type_ed
                TYPE_PV -> R.string.episode_type_pv
                TYPE_MAD -> R.string.episode_type_mad
                TYPE_MUSIC -> R.string.episode_type_music
                else -> R.string.episode_type_other
            }
        }
    }
}