package soko.ekibun.bangumi.plugins.scripts.hanhan

import soko.ekibun.bangumi.plugins.MangaScriptTest
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.manga.MangaProvider

class TestData : MangaScriptTest.MangaTestData {
    override val info = LineProvider.ProviderInfo(
        site = "hanhan",
        color = 0x003333,
        title = "汗汗酷漫",
        type = Provider.TYPE_MANGA
    )
    override val searchKey = "日常"
    override val lineInfo = LineInfoModel.LineInfo(
        "test",
        id = "36056",
        extra = ""
    )
    override val episode = MangaProvider.MangaEpisode(
        site = "hanhan",
        id = "/cool323156/1.html?s=8",
        sort = "001集",
        title = "机械依存系少女 001集",
        url = "http://www.hhimm.com/cool323156/1.html?s=8"
    )
    override val image = MangaProvider.ImageInfo(
        url = "http://www.hhimm.com/cool376511/1.html?s=3"
    )
}