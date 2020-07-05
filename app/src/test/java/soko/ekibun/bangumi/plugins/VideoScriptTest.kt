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

/**
 * 视频测试
 */
class VideoScriptTest {

    abstract class VideoTestData : ScriptTest.BaseTestData<VideoProvider> {
        @ExperimentalCoroutinesApi
        @get:Rule
        var mainCoroutineRule = MainCoroutineRule()
        override val providerClass = VideoProvider::class.java

        open val searchKey: String? = null
        open val lineInfo: LineInfo? = null
        open val episode: Episode = Episode(
            sort = 1f
        )
        open val video: VideoProvider.VideoInfo? = null
        open val danmakuKey: String = JsonUtil.toJson("")

        val provider by lazy { ScriptTest.getProvider(this) }

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
            println(ProviderInfo.toUrl(listOf(info.also {
                it.code = JsonUtil.toJson(ScriptTest.getProvider(this))
            })))
        }
    }
}
