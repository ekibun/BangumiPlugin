package soko.ekibun.bangumi.plugins.provider.book

import soko.ekibun.bangumi.plugins.engine.JsEngine
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.util.JsonUtil

class BookProvider(
    search: String? = null,
    @Code("获取更新列表", 1) val getUpdate: String? = "",  // () -> List<AirInfo>
    @Code("获取剧集列表", 2) val getEpisode: String? = "", // (line: LineInfo) -> List<BookEpisode>
    @Code("获取页面信息", 3) val getPages: String? = "",   // (episode: BookEpisode) -> List<PageInfo>
    @Code("获取图片", 4) val getImage: String? = ""        // (image: ImageInfo) -> HttpRequest
) : Provider(search) {
    suspend fun getEpisode(
        scriptKey: String,
        line: LineInfo
    ): List<BookEpisode> {
        return JsEngine.makeScript("var line = ${JsonUtil.toJson(line)};\n$getEpisode", header, scriptKey) {
            JsonUtil.toEntity<List<BookEpisode>>(it)!!
        }
    }

    suspend fun getUpdate(scriptKey: String): List<AirInfo> {
        return JsEngine.makeScript(
            getUpdate ?: "",
            header,
            scriptKey
        ) {
            JsonUtil.toEntity<List<AirInfo>>(it)!!
        }
    }

    suspend fun getPages(scriptKey: String, episode: BookEpisode): List<PageInfo> {
        return JsEngine.makeScript(
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

    suspend fun getImage(scriptKey: String, page: PageInfo): HttpRequest {
        return JsEngine.makeScript(
            "var page = ${JsonUtil.toJson(page)};\n${
            if (!getImage.isNullOrEmpty()) getImage else "return page.image;"}", header, scriptKey
        ) {
            JsonUtil.toEntity<HttpRequest>(it)!!
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
        val image: HttpRequest? = null,
        val content: String? = null,
        var ep: BookEpisode? = null,
        var index: Int = 0
    )

    data class AirInfo(
        val site: String? = null,
        val id: String? = null,
        val air: String? = null
    )

    override fun createPluginView(linePresenter: LinePresenter): PluginView {
        return BookPluginView(linePresenter)
    }
}