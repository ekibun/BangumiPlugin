package soko.ekibun.bangumi.plugins.provider.music

import soko.ekibun.bangumi.plugins.engine.JsEngine
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.subject.LinePresenter
import soko.ekibun.bangumi.plugins.util.JsonUtil


class MusicProvider(
    @Code("获取音乐", 1) val getMusic: String? = "",   // (episode: PluginEpisode) -> HttpRequest
    @Code("获取歌词", 2) val getLyric: String? = ""    // (episode: PluginEpisode) -> Lyric
) : Provider.EpisodeProvider() {

    suspend fun getMusic(scriptKey: String, episode: ProviderEpisode): HttpRequest {
        return JsEngine.makeScript(
            "var episode = ${JsonUtil.toJson(episode)};\n${if (!getMusic.isNullOrEmpty()) getMusic else "return http.webview(episode.url);"}",
            header,
            scriptKey
        ) {
            JsonUtil.toEntity<HttpRequest>(it)!!
        }
    }

    suspend fun getLyric(scriptKey: String, episode: ProviderEpisode): Lyric {
        return JsEngine.makeScript(
            "var episode = ${JsonUtil.toJson(episode)};\n$getLyric", header, scriptKey
        ) {
            JsonUtil.toEntity<Lyric>(it)!!
        }
    }

    data class Lyric(
        val lrc: String? = null,
        val tlyric: String? = null
    )

    override fun createPluginView(linePresenter: LinePresenter): PluginView {
        return MusicPluginView(linePresenter)
    }
}