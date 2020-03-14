package soko.ekibun.bangumi.plugins.provider.book

import soko.ekibun.bangumi.plugins.JsEngine
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.util.HttpUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil

class BookProvider(
    search: String? = null,
    @Code("获取剧集列表", 1) val getEpisode: String? = "", // (line: LineInfo) -> List<BookEpisode>
    @Code("获取页面信息", 2) val getPages: String? = "",   // (episode: BookEpisode) -> List<PageInfo>
    @Code("获取图片", 3) val getImage: String? = ""        // (image: ImageInfo) -> HttpRequest
) : Provider(search) {
    fun getEpisode(
        scriptKey: String,
        jsEngine: JsEngine,
        line: LineInfoModel.LineInfo
    ): JsEngine.ScriptTask<List<BookEpisode>> {
        return JsEngine.ScriptTask(jsEngine, "var line = ${JsonUtil.toJson(line)};\n$getEpisode", header, scriptKey) {
            JsonUtil.toEntity<List<BookEpisode>>(it)!!
        }
    }

    fun getPages(scriptKey: String, jsEngine: JsEngine, episode: BookEpisode): JsEngine.ScriptTask<List<PageInfo>> {
        return JsEngine.ScriptTask(
            jsEngine,
            "var episode = ${JsonUtil.toJson(episode)};\n$getPages",
            header,
            scriptKey
        ) {
            val ret = JsonUtil.toEntity<List<PageInfo>>(it)!!
            ret.forEachIndexed { index, imageInfo ->
                imageInfo.ep = episode
                imageInfo.index = index + 1
            }
            ret
        }
    }

    fun getImage(scriptKey: String, jsEngine: JsEngine, page: PageInfo): JsEngine.ScriptTask<HttpUtil.HttpRequest> {
        return JsEngine.ScriptTask(
            jsEngine, "var page = ${JsonUtil.toJson(page)};\n${
            if (!getImage.isNullOrEmpty()) getImage else "return page.image;"}", header, scriptKey
        ) {
            JsonUtil.toEntity<HttpUtil.HttpRequest>(it)!!
        }
    }

    data class BookEpisode(
        val site: String,
        val id: String,
        val sort: Float,
        var category: String? = null,
        val title: String,
        val url: String
    )

    data class PageInfo(
        val site: String? = null,
        val image: HttpUtil.HttpRequest? = null,
        val content: String? = null,
        var ep: BookEpisode? = null,
        var index: Int = 0
    )

    override fun createPluginView(linePresenter: LinePresenter): PluginView {
        return BookPluginView(linePresenter)
    }
}