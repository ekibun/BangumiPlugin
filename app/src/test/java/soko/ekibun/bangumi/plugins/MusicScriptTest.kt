package soko.ekibun.bangumi.plugins

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.music.MusicProvider
import soko.ekibun.bangumi.plugins.util.JsonUtil

/**
 * 音乐测试
 */
class MusicScriptTest {

    abstract class MusicTestData : ScriptTest.BaseTestData<MusicProvider> {
        @ExperimentalCoroutinesApi
        @get:Rule
        var mainCoroutineRule = MainCoroutineRule()
        override val providerClass = MusicProvider::class.java

        open val searchKey: String? = null
        open val lineInfo: LineInfo? = null
        open val episode: Provider.ProviderEpisode? = null

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
        fun getEpisode() = mainCoroutineRule.runBlockingTest {
            if (provider.getEpisode.isNullOrEmpty()) println("no getEpisode script!")
            else println(JsonUtil.toJson(provider.getEpisode("test", lineInfo!!)))
        }

        @Test
        @ExperimentalCoroutinesApi
        fun getMusic() = mainCoroutineRule.runBlockingTest {
            if (provider.getMusic.isNullOrEmpty()) println("no getMusic script!")
            else println(JsonUtil.toJson(provider.getMusic("test", episode!!)))
        }

        @Test
        @ExperimentalCoroutinesApi
        fun getLyric() = mainCoroutineRule.runBlockingTest {
            if (provider.getLyric.isNullOrEmpty()) println("no getLyric script!")
            else println(JsonUtil.toJson(provider.getLyric("test", episode!!)))
        }

        @Test
        fun printProvider() {
            println(JsonUtil.toJson(info.also {
                it.code = JsonUtil.toJson(ScriptTest.getProvider(this))
            }))
        }
    }
}
