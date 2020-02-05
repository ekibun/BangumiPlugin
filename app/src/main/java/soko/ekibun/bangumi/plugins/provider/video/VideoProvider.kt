package soko.ekibun.bangumi.plugins.provider.video

import android.util.Log
import soko.ekibun.bangumi.plugins.JsEngine
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.util.HttpUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil

class VideoProvider(
    search: String? = null,
    @Code("获取剧集信息", 1) val getVideoInfo: String? = "",  // (line: LineInfo, episode: VideoEpisode) -> VideoInfo
    @Code("获取视频信息", 2) val getVideo: String? = "",      // (video: VideoInfo) -> HttpRequest
    @Code("获取弹幕信息", 3) val getDanmakuKey: String? = "", // (video: VideoInfo) -> Object
    @Code("获取弹幕", 4) val getDanmaku: String? = ""     // (video: VideoInfo, key: Object, pos: Int) -> List<DanmakuInfo>
): Provider(search){
    fun getVideoInfo(scriptKey: String, jsEngine: JsEngine, line: LineInfoModel.LineInfo, episode: Episode): JsEngine.ScriptTask<VideoInfo>{
        return JsEngine.ScriptTask(jsEngine,"var line = ${JsonUtil.toJson(line)};var episode = ${JsonUtil.toJson(episode)};\n$getVideoInfo", scriptKey){
            JsonUtil.toEntity<VideoInfo>(it)!!
        }
    }

    fun getVideo(scriptKey: String, jsEngine: JsEngine, video: VideoInfo): JsEngine.ScriptTask<HttpUtil.HttpRequest>{
        return JsEngine.ScriptTask(jsEngine,"var video = ${JsonUtil.toJson(video)};\n${if(!getVideo.isNullOrEmpty()) getVideo else "return webview.load(video.url);"}", scriptKey){
            JsonUtil.toEntity<HttpUtil.HttpRequest>(it)!!
        }
    }

    fun getDanmakuKey(scriptKey: String, jsEngine: JsEngine, video: VideoInfo): JsEngine.ScriptTask<String>{
        return JsEngine.ScriptTask(jsEngine,"var video = ${JsonUtil.toJson(video)};\n${if(!getDanmakuKey.isNullOrEmpty()) getDanmakuKey else "return \"\";"}", scriptKey){
            Log.v("plugin", "danmaku $it")
            it
        }
    }

    fun getDanmaku(scriptKey: String, jsEngine: JsEngine, video: VideoInfo, key: String, pos: Int): JsEngine.ScriptTask<List<DanmakuInfo>>{
        return JsEngine.ScriptTask(jsEngine,"var video = ${JsonUtil.toJson(video)};var key = $key;var pos = $pos;\n$getDanmaku", scriptKey){
            Log.v("plugin", "danmaku $it")
            JsonUtil.toEntity<List<DanmakuInfo>>(it)?:ArrayList()
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