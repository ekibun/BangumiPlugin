package soko.ekibun.bangumi.plugins

import org.junit.Test
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.manga.MangaProvider
import soko.ekibun.bangumi.plugins.util.JsonUtil

/**
 * 漫画测试
 */
class MangaScriptTest {

    interface MangaTestData {
        val info: LineProvider.ProviderInfo
        val searchKey: String
        val lineInfo: LineInfoModel.LineInfo
        val episode: MangaProvider.MangaEpisode
        val image: MangaProvider.ImageInfo
    }

    val testData = soko.ekibun.bangumi.plugins.scripts.dmzj.TestData()

    @Test
    fun search() {
        if (provider.search.isNullOrEmpty()) println("no search script!")
        else println(JsonUtil.toJson(provider.search("test", ScriptTest.jsEngine, testData.searchKey).runScript()))
    }

    @Test
    fun getEpisode() {
        if (provider.getEpisode.isNullOrEmpty()) println("no getEpisode script!")
        else println(JsonUtil.toJson(provider.getEpisode("test", ScriptTest.jsEngine, testData.lineInfo).runScript()))
    }

    @Test
    fun getManga() {
        if (provider.getManga.isNullOrEmpty()) println("no getManga script!")
        else println(JsonUtil.toJson(provider.getManga("test", ScriptTest.jsEngine, testData.episode).runScript()))
    }

    @Test
    fun getImage() {
        if (provider.getImage.isNullOrEmpty()) println("no getImage script!")
        else println(JsonUtil.toJson(provider.getImage("test", ScriptTest.jsEngine, testData.image).runScript()))
    }

    @Test
    fun printProvider() {
        testData.info.code = JsonUtil.toJson(provider)
        println(JsonUtil.toJson(testData.info))
    }

    val provider = ScriptTest.getProvider<MangaProvider>(testData.info.site)
}
