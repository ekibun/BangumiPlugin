package soko.ekibun.bangumi.plugins

import org.junit.Test
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.video.VideoProvider
import soko.ekibun.bangumi.plugins.util.JsonUtil
import java.io.File

/**
 * 视频测试
 */
class VideoScriptTest {

    abstract class VideoTestData {
        abstract val info: LineProvider.ProviderInfo
        open val searchKey: String? = null
        open val lineInfo: LineInfoModel.LineInfo? = null
        open val episode: Episode = Episode(
            sort = 1f
        )
        open val video: VideoProvider.VideoInfo? = null
        open val danmakuKey: String = JsonUtil.toJson("")
    }

    val testData: VideoTestData = soko.ekibun.bangumi.plugins.scripts.agefans.TestData()
    val provider = ScriptTest.getProvider<VideoProvider>(testData.info.site)

    @Test
    fun search() {
        if (provider.search.isNullOrEmpty()) println("no search script!")
        else println(JsonUtil.toJson(provider.search("test", ScriptTest.jsEngine, testData.searchKey!!).runScript()))
    }

    @Test
    fun getVideoInfo() {
        if (provider.getVideoInfo.isNullOrEmpty()) println("no getVideoInfo script!")
        else println(
            JsonUtil.toJson(
                provider.getVideoInfo(
                    "test",
                    ScriptTest.jsEngine,
                    testData.lineInfo!!,
                    testData.episode
                ).runScript()
            )
        )
    }

    @Test
    fun getVideo() {
        if (provider.getVideo.isNullOrEmpty()) println("no getVideo script!")
        else println(JsonUtil.toJson(provider.getVideo("test", ScriptTest.jsEngine, testData.video!!).runScript()))
    }

    @Test
    fun getDanmakuKey() {
        if (provider.getDanmakuKey.isNullOrEmpty()) println("no getDanmakuKey script!")
        else println(provider.getDanmakuKey("test", ScriptTest.jsEngine, testData.video!!).runScript())
    }

    @Test
    fun getDanmaku() {
        if (provider.getDanmaku.isNullOrEmpty()) println("no getDanmaku script!")
        else println(
            JsonUtil.toJson(
                provider.getDanmaku(
                    "test",
                    ScriptTest.jsEngine,
                    testData.video!!,
                    testData.danmakuKey,
                    0
                ).runScript()
            )
        )
    }

    @Test
    fun printProvider() {
        println(JsonUtil.toJson(testData.info.also {
            it.code = JsonUtil.toJson(ScriptTest.getProvider<VideoProvider>(it.site))
        }))
    }

    @Test
    fun writeProvider() {
        val file = File("${ScriptTest.SCRIPT_PATH}/videos.json")
        file.writeText(JsonUtil.toJson(scriptList.map {
            it.info.code = JsonUtil.toJson(ScriptTest.getProvider<VideoProvider>(it.info.site))
            it.info
        }))
    }

    val scriptList = arrayOf(
        soko.ekibun.bangumi.plugins.scripts.acfun.TestData(),
        soko.ekibun.bangumi.plugins.scripts.agefans.TestData(),
        soko.ekibun.bangumi.plugins.scripts.bilibili.TestData(),
        soko.ekibun.bangumi.plugins.scripts.fodm.TestData(),
        soko.ekibun.bangumi.plugins.scripts.iqiyi.TestData(),
        soko.ekibun.bangumi.plugins.scripts.nicotv.TestData(),
        soko.ekibun.bangumi.plugins.scripts.ningmoe.TestData(),
        soko.ekibun.bangumi.plugins.scripts.tencent.TestData(),
        soko.ekibun.bangumi.plugins.scripts.webpage.TestData()
    )
}
