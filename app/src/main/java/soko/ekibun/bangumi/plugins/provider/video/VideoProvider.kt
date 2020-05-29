package soko.ekibun.bangumi.plugins.provider.video

import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.engine.JsEngine
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.util.JsonUtil

class VideoProvider(
    search: String? = null,
    @Code("获取剧集信息", 1) val getVideoInfo: String? = "",  // (line: LineInfo, episode: VideoEpisode) -> VideoInfo
    @Code("获取视频信息", 2) val getVideo: String? = "",      // (video: VideoInfo) -> HttpRequest
    @Code("获取弹幕信息", 3) val getDanmakuKey: String? = "", // (video: VideoInfo) -> Object
    @Code("获取弹幕", 4) val getDanmaku: String? = ""     // (video: VideoInfo, key: Object, pos: Int) -> List<DanmakuInfo>
): Provider(search) {
    suspend fun getVideoInfo(
        scriptKey: String,
        line: LineInfo,
        episode: Episode
    ): VideoInfo {
        return JsEngine.makeScript(
            "var line = ${JsonUtil.toJson(line)};var episode = ${JsonUtil.toJson(episode)};\n$getVideoInfo",
            header,
            scriptKey
        ) {
            JsonUtil.toEntity<VideoInfo>(it)!!
        }
    }

    suspend fun getVideo(scriptKey: String, video: VideoInfo): HttpRequest {
        return JsEngine.makeScript(
            "var video = ${JsonUtil.toJson(video)};\n${if (!getVideo.isNullOrEmpty()) getVideo else "return http.webview(video.url);"}",
            header,
            scriptKey
        ) {
            JsonUtil.toEntity<HttpRequest>(it)!!
        }
    }

    suspend fun getDanmakuKey(scriptKey: String, video: VideoInfo): String {
        return JsEngine.makeScript(
            "var video = ${JsonUtil.toJson(video)};\n${if (!getDanmakuKey.isNullOrEmpty()) getDanmakuKey else "return \"\";"}",
            header,
            scriptKey
        ) { it }
    }

    suspend fun getDanmaku(
        scriptKey: String,
        video: VideoInfo,
        key: String,
        pos: Int
    ): List<DanmakuInfo> {
        return JsEngine.makeScript(
            "var video = ${JsonUtil.toJson(video)};var key = $key;var pos = $pos;\n$getDanmaku",
            header,
            scriptKey
        ) {
            JsonUtil.toEntity<List<DanmakuInfo>>(it) ?: ArrayList()
        }
    }

    data class VideoInfo(
        val site: String,
        val id: String,
        val url: String
    )

    data class DanmakuInfo(
        val time: Float,
        val type: Int,
        val textSize: Float,
        val color: Int,
        val content: String,
        val timeStamp: Long = 0L
    )

    override fun createPluginView(linePresenter: LinePresenter): PluginView {
        return VideoPluginView(linePresenter)
    }
}