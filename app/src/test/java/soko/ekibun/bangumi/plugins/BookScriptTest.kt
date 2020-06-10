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
import java.io.File

/**
 * 漫画测试
 */
class BookScriptTest {

    abstract class BookTestData {
        @ExperimentalCoroutinesApi
        @get:Rule
        var mainCoroutineRule = MainCoroutineRule()

        abstract val info: ProviderInfo
        open val searchKey: String? = null
        open val lineInfo: LineInfo? = null
        open val episode: Provider.ProviderEpisode? = null
        open val page: BookProvider.PageInfo? = null

        val provider by lazy { ScriptTest.getProvider<BookProvider>(info.site) }

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
            println(JsonUtil.toJson(info.also {
                it.code = JsonUtil.toJson(ScriptTest.getProvider<BookProvider>(it.site))
            }))
        }
    }

    @Test
    fun writeProvider() {
        val file = File("${ScriptTest.SCRIPT_PATH}/books.json")
        file.writeText(JsonUtil.toJson(scriptList.map {
            it.info.code = JsonUtil.toJson(ScriptTest.getProvider<BookProvider>(it.info.site))
            it.info
        }))
    }

    val scriptList = arrayOf(
        soko.ekibun.bangumi.plugins.scripts.dmzj.TestData(),
        soko.ekibun.bangumi.plugins.scripts.hanhan.TestData(),
        soko.ekibun.bangumi.plugins.scripts.mangabz.TestData(),
        soko.ekibun.bangumi.plugins.scripts.manhua123.TestData(),
        soko.ekibun.bangumi.plugins.scripts.manhuabei.TestData(),
        soko.ekibun.bangumi.plugins.scripts.manhuagui.TestData(),
        soko.ekibun.bangumi.plugins.scripts.mh177.TestData(),
        soko.ekibun.bangumi.plugins.scripts.pica.TestData(),
        soko.ekibun.bangumi.plugins.scripts.wenku8.TestData()
    )
}
