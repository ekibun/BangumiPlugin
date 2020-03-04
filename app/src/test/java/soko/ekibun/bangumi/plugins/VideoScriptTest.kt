package soko.ekibun.bangumi.plugins

import org.junit.Test
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.video.VideoProvider
import soko.ekibun.bangumi.plugins.util.JsonUtil

/**
 * 视频测试
 */
class VideoScriptTest {
    /**
     * 线路配置
     */
    private val info = LineProvider.ProviderInfo(
        site = "acfun",
        color = 0xfd4c5b,
        title = "Acfun",
        type = Provider.TYPE_VIDEO
    )
    /**
     * 测试用的数据
     */
    private val searchKey = "房间露营"
    private val lineInfo = LineInfoModel.LineInfo(
        "test",
        id = "6000901"
    )
    private val episode = Episode(
        sort = 1f
    )
    private val video = VideoProvider.VideoInfo(
        site = "test",
        id = "11188351",
        url = "http://www.acfun.cn/bangumi/aa6000901_35425_1707941"
    )
    private val danmakuKey = JsonUtil.toJson("")

    @Test
    fun search() {
        if (provider.search.isNullOrEmpty()) println("no search script!")
        else println(JsonUtil.toJson(provider.search("test", ScriptTest.jsEngine, searchKey).runScript()))
    }

    @Test
    fun getVideoInfo() {
        if (provider.getVideoInfo.isNullOrEmpty()) println("no getVideoInfo script!")
        else println(JsonUtil.toJson(provider.getVideoInfo("test", ScriptTest.jsEngine, lineInfo, episode).runScript()))
    }

    @Test
    fun getVideo() {
        if (provider.getVideo.isNullOrEmpty()) println("no getVideo script!")
        else println(JsonUtil.toJson(provider.getVideo("test", ScriptTest.jsEngine, video).runScript()))
    }

    @Test
    fun getDanmakuKey() {
        if (provider.getDanmakuKey.isNullOrEmpty()) println("no getDanmakuKey script!")
        else println(provider.getDanmakuKey("test", ScriptTest.jsEngine, video).runScript())
    }

    @Test
    fun getDanmaku() {
        if (provider.getDanmaku.isNullOrEmpty()) println("no getDanmaku script!")
        else println(
            JsonUtil.toJson(
                provider.getDanmaku(
                    "test",
                    ScriptTest.jsEngine,
                    video,
                    danmakuKey,
                    0
                ).runScript()
            )
        )
    }

    @Test
    fun printProvider() {
        info.code = JsonUtil.toJson(provider)
        println(JsonUtil.toJson(info))
    }

    val provider = ScriptTest.getProvider<VideoProvider>(info.site)
}
