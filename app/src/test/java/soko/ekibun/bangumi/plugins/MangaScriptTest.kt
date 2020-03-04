package soko.ekibun.bangumi.plugins

import org.junit.Test
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.manga.MangaProvider
import soko.ekibun.bangumi.plugins.util.JsonUtil

/**
 * 漫画测试
 */
class MangaScriptTest {
    /**
     * 线路配置
     */
    private val info = LineProvider.ProviderInfo(
        site = "hanhan",
        color = 0x003333,
        title = "汗汗酷漫",
        type = Provider.TYPE_MANGA
    )
    /**
     * 测试用的数据
     */
    private val searchKey = "日常"
    private val lineInfo = LineInfoModel.LineInfo(
        "test",
        id = "36056",
        extra = ""
    )
    private val episode = MangaProvider.MangaEpisode(
        site = "hanhan",
        id = "/cool323156/1.html?s=8",
        sort = "001集",
        title = "机械依存系少女 001集",
        url = "http://www.hhimm.com/cool323156/1.html?s=8"
    )
    private val image = MangaProvider.ImageInfo(
        url = "http://www.hhimm.com/cool376511/1.html?s=3"
    )

    @Test
    fun search() {
        if (provider.search.isNullOrEmpty()) println("no search script!")
        else println(JsonUtil.toJson(provider.search("test", ScriptTest.jsEngine, searchKey).runScript()))
    }

    @Test
    fun getEpisode() {
        if (provider.getEpisode.isNullOrEmpty()) println("no getEpisode script!")
        else println(JsonUtil.toJson(provider.getEpisode("test", ScriptTest.jsEngine, lineInfo).runScript()))
    }

    @Test
    fun getManga() {
        if (provider.getManga.isNullOrEmpty()) println("no getManga script!")
        else println(JsonUtil.toJson(provider.getManga("test", ScriptTest.jsEngine, episode).runScript()))
    }

    @Test
    fun getImage() {
        if (provider.getImage.isNullOrEmpty()) println("no getImage script!")
        else println(JsonUtil.toJson(provider.getImage("test", ScriptTest.jsEngine, image).runScript()))
    }

    @Test
    fun printProvider() {
        info.code = JsonUtil.toJson(provider)
        println(JsonUtil.toJson(info))
    }

    val provider = ScriptTest.getProvider<MangaProvider>(info.site)
}
