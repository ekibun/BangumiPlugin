package soko.ekibun.bangumi.plugins

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.video.VideoProvider
import soko.ekibun.bangumi.plugins.util.JsonUtil
import java.io.File

/**
 * 视频测试
 */
class VideoScriptTest {

    abstract class VideoTestData {
        @ExperimentalCoroutinesApi
        @get:Rule
        var mainCoroutineRule = MainCoroutineRule()

        abstract val info: ProviderInfo
        open val searchKey: String? = null
        open val lineInfo: LineInfo? = null
        open val episode: Episode = Episode(
            sort = 1f
        )
        open val video: VideoProvider.VideoInfo? = null
        open val danmakuKey: String = JsonUtil.toJson("")

        val provider by lazy { ScriptTest.getProvider<VideoProvider>(info.site) }

        @Test
        @ExperimentalCoroutinesApi
        fun search() = mainCoroutineRule.runBlockingTest {
            if (provider.search.isNullOrEmpty()) println("no search script!")
            else println(
                JsonUtil.toJson(
                    provider.search("test", searchKey!!)
                )
            )
        }

        @Test
        @ExperimentalCoroutinesApi
        fun getVideoInfo() = mainCoroutineRule.runBlockingTest {
            if (provider.getVideoInfo.isNullOrEmpty()) println("no getVideoInfo script!")
            else println(
                JsonUtil.toJson(
                    provider.getVideoInfo(
                        "test",
                        lineInfo!!,
                        episode
                    )
                )
            )
        }

        @Test
        @ExperimentalCoroutinesApi
        fun getVideo() = mainCoroutineRule.runBlockingTest {
            if (provider.getVideo.isNullOrEmpty()) println("no getVideo script!")
            else println(JsonUtil.toJson(provider.getVideo("test", video!!)))
        }

        @Test
        @ExperimentalCoroutinesApi
        fun getDanmakuKey() = mainCoroutineRule.runBlockingTest {
            if (provider.getDanmakuKey.isNullOrEmpty()) println("no getDanmakuKey script!")
            else println(provider.getDanmakuKey("test", video!!))
        }

        @Test
        @ExperimentalCoroutinesApi
        fun getDanmaku() = mainCoroutineRule.runBlockingTest {
            if (provider.getDanmaku.isNullOrEmpty()) println("no getDanmaku script!")
            else println(JsonUtil.toJson(provider.getDanmaku("test", video!!, danmakuKey, 0)))
        }

        @Test
        fun printProvider() {
            println(JsonUtil.toJson(info.also {
                it.code = JsonUtil.toJson(ScriptTest.getProvider<VideoProvider>(it.site))
            }))
        }
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
