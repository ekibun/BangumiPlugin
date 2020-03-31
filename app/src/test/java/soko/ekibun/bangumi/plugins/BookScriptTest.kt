package soko.ekibun.bangumi.plugins

import org.junit.Test
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.book.BookProvider
import soko.ekibun.bangumi.plugins.util.JsonUtil
import java.io.File

/**
 * 漫画测试
 */
class BookScriptTest {

    abstract class BookTestData {
        abstract val info: LineProvider.ProviderInfo
        open val searchKey: String? = null
        open val lineInfo: LineInfoModel.LineInfo? = null
        open val episode: BookProvider.BookEpisode? = null
        open val page: BookProvider.PageInfo? = null
    }

    val testData: BookTestData = soko.ekibun.bangumi.plugins.scripts.manhua123.TestData()
    val provider = ScriptTest.getProvider<BookProvider>(testData.info.site)

    @Test
    fun search() {
        if (provider.search.isNullOrEmpty()) println("no search script!")
        else println(JsonUtil.toJson(provider.search("test", ScriptTest.jsEngine, testData.searchKey!!).runScript()))
    }

    @Test
    fun getEpisode() {
        if (provider.getEpisode.isNullOrEmpty()) println("no getEpisode script!")
        else println(JsonUtil.toJson(provider.getEpisode("test", ScriptTest.jsEngine, testData.lineInfo!!).runScript()))
    }

    @Test
    fun getPages() {
        if (provider.getPages.isNullOrEmpty()) println("no getPages script!")
        else println(JsonUtil.toJson(provider.getPages("test", ScriptTest.jsEngine, testData.episode!!).runScript()))
    }

    @Test
    fun getImage() {
        if (provider.getImage.isNullOrEmpty()) println("no getImage script!")
        else println(JsonUtil.toJson(provider.getImage("test", ScriptTest.jsEngine, testData.page!!).runScript()))
    }

    @Test
    fun printProvider() {
        println(JsonUtil.toJson(testData.info.also {
            it.code = JsonUtil.toJson(ScriptTest.getProvider<BookProvider>(it.site))
        }))
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
        soko.ekibun.bangumi.plugins.scripts.manhuadui.TestData(),
        soko.ekibun.bangumi.plugins.scripts.manhuagui.TestData(),
        soko.ekibun.bangumi.plugins.scripts.mh177.TestData(),
        soko.ekibun.bangumi.plugins.scripts.pica.TestData(),
        soko.ekibun.bangumi.plugins.scripts.wenku8.TestData()
    )
}
