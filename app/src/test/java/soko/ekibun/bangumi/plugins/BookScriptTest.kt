package soko.ekibun.bangumi.plugins

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.book.BookProvider
import soko.ekibun.bangumi.plugins.util.JsonUtil

/**
 * 漫画测试
 */
class BookScriptTest {

    abstract class BookTestData : ScriptTest.BaseTestData<BookProvider> {
        @ExperimentalCoroutinesApi
        @get:Rule
        var mainCoroutineRule = MainCoroutineRule()
        override val providerClass = BookProvider::class.java

        open val searchKey: String? = null
        open val lineInfo: LineInfo? = null
        open val episode: Provider.ProviderEpisode? = null
        open val page: BookProvider.PageInfo? = null

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
        fun getUpdate() = mainCoroutineRule.runBlockingTest {
            if (provider.getUpdate.isNullOrEmpty()) println("no getUpdate script!")
            else println(JsonUtil.toJson(provider.getUpdate("test")))
        }

        @Test
        @ExperimentalCoroutinesApi
        fun getEpisode() = mainCoroutineRule.runBlockingTest {
            if (provider.getEpisode.isNullOrEmpty()) println("no getEpisode script!")
            else println(
                JsonUtil.toJson(
                    provider.getEpisode("test", lineInfo!!)
                )
            )
        }

        @Test
        @ExperimentalCoroutinesApi
        fun getPages() = mainCoroutineRule.runBlockingTest {
            if (provider.getPages.isNullOrEmpty()) println("no getPages script!")
            else println(
                JsonUtil.toJson(
                    provider.getPages("test", episode!!)
                )
            )
        }

        @Test
        @ExperimentalCoroutinesApi
        fun getImage() = mainCoroutineRule.runBlockingTest {
            if (provider.getImage.isNullOrEmpty()) println("no getImage script!")
            else println(JsonUtil.toJson(provider.getImage("test", page!!)))
        }

        @Test
        fun printProvider() {
            println(ProviderInfo.toUrl(listOf(info.also {
                it.code = JsonUtil.toJson(ScriptTest.getProvider(this))
            })))
        }
    }
}
