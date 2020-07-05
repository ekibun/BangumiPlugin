package soko.ekibun.bangumi.plugins.scripts.book.gamertw

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "gamertw",
        color = 0x198be3,
        title = "巴哈姆特",
        type = Provider.TYPE_BOOK
    )
    override val lineInfo = LineInfo(
        "gamertw",
        id = "https://home.gamer.com.tw/creationCategory.php?v=1&owner=a4122919&c=420071",
        extra = ""
    )
    override val episode = Provider.ProviderEpisode(
        site = "dmzj",
        id = "59967",
        sort = 1f,
        title = "第01话",
        url = "https://m.dmzj.com/view/40523/59967.html"
    )
}