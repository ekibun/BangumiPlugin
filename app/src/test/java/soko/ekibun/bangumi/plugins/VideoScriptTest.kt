package soko.ekibun.bangumi.plugins

import org.junit.Test
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.video.VideoProvider
import soko.ekibun.bangumi.plugins.util.JsonUtil

/**
 * 视频测试
 */
class VideoScriptTest {

    interface VideoTestData {
        val info: LineProvider.ProviderInfo
        val searchKey: String
        val lineInfo: LineInfoModel.LineInfo
        val episode: Episode
        val video: VideoProvider.VideoInfo
        val danmakuKey: String
    }

    val testData = soko.ekibun.bangumi.plugins.scripts.acfun.TestData()

    @Test
    fun search() {
        if (provider.search.isNullOrEmpty()) println("no search script!")
        else println(JsonUtil.toJson(provider.search("test", ScriptTest.jsEngine, testData.searchKey).runScript()))
    }

    @Test
    fun getVideoInfo() {
        if (provider.getVideoInfo.isNullOrEmpty()) println("no getVideoInfo script!")
        else println(
            JsonUtil.toJson(
                provider.getVideoInfo(
                    "test",
                    ScriptTest.jsEngine,
                    testData.lineInfo,
                    testData.episode
                ).runScript()
            )
        )
    }

    @Test
    fun getVideo() {
        if (provider.getVideo.isNullOrEmpty()) println("no getVideo script!")
        else println(JsonUtil.toJson(provider.getVideo("test", ScriptTest.jsEngine, testData.video).runScript()))
    }

    @Test
    fun getDanmakuKey() {
        if (provider.getDanmakuKey.isNullOrEmpty()) println("no getDanmakuKey script!")
        else println(provider.getDanmakuKey("test", ScriptTest.jsEngine, testData.video).runScript())
    }

    @Test
    fun getDanmaku() {
        if (provider.getDanmaku.isNullOrEmpty()) println("no getDanmaku script!")
        else println(
            JsonUtil.toJson(
                provider.getDanmaku(
                    "test",
                    ScriptTest.jsEngine,
                    testData.video,
                    testData.danmakuKey,
                    0
                ).runScript()
            )
        )
    }

    @Test
    fun printProvider() {
        testData.info.code = JsonUtil.toJson(provider)
        println(JsonUtil.toJson(testData.info))
    }

    val provider = ScriptTest.getProvider<VideoProvider>(testData.info.site)
}
