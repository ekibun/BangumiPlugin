package soko.ekibun.bangumi.plugins.provider.manga

import soko.ekibun.bangumi.plugins.JsEngine
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.util.HttpUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil

class MangaProvider(
    search: String? = null,
    @Code("获取剧集列表", 1) val getEpisode: String = "", // (line: LineInfo) -> List<MangaEpisode>
    @Code("获取图片列表", 2) val getManga: String = "",   // (episode: MangaEpisode) -> List<ImageInfo>
    @Code("获取图片", 3) val getImage: String = ""        // (image: ImageInfo) -> HttpRequest
): Provider(search) {
    fun getEpisode(scriptKey: String, jsEngine: JsEngine, line: LineInfoModel.LineInfo): JsEngine.ScriptTask<List<MangaEpisode>> {
        return JsEngine.ScriptTask(jsEngine, "var line = ${JsonUtil.toJson(line)};\n$getEpisode", header, scriptKey) {
            JsonUtil.toEntity<List<MangaEpisode>>(it)!!
        }
    }
    fun getManga(scriptKey: String, jsEngine: JsEngine, episode: MangaEpisode): JsEngine.ScriptTask<List<ImageInfo>> {
        return JsEngine.ScriptTask(
            jsEngine,
            "var episode = ${JsonUtil.toJson(episode)};\n$getManga",
            header,
            scriptKey
        ) {
            val ret = JsonUtil.toEntity<List<ImageInfo>>(it)!!
            ret.forEachIndexed { index, imageInfo ->
                imageInfo.ep = episode
                imageInfo.index = index + 1
            }
            ret
        }
    }
    fun getImage(scriptKey: String, jsEngine: JsEngine, image: ImageInfo): JsEngine.ScriptTask<HttpUtil.HttpRequest> {
        return JsEngine.ScriptTask(
            jsEngine, "var image = ${JsonUtil.toJson(image)};\n${
            if (getImage.isNotEmpty()) getImage else "return image.url;"}", header, scriptKey
        ) {
            JsonUtil.toEntity<HttpUtil.HttpRequest>(it)!!
        }
    }

    data class MangaEpisode(
        val site: String,
        val id: String,
        val sort: String,
        val title: String,
        val url: String
    )

    data class ImageInfo(
        val site: String?,
        val id: String?,
        val url: String,
        var ep: MangaEpisode? = null,
        var index: Int = 0
    )

    override fun createPluginView(linePresenter: LinePresenter): PluginView {
        return MangaPluginView(linePresenter)
    }
}