package soko.ekibun.bangumi.plugins.bean

import android.os.Parcel
import android.os.Parcelable
import com.google.android.exoplayer2.offline.StreamKey
import soko.ekibun.bangumi.plugins.util.HttpUtil

data class VideoCache(
    val episode: Episode,
    val type: String,
    val streamKeys: List<StreamKey>,
    val video: HttpUtil.HttpRequest,
    var contentLength: Long = 0L,
    var bytesDownloaded: Long = 0L,
    var percentDownloaded: Float = 0f
)