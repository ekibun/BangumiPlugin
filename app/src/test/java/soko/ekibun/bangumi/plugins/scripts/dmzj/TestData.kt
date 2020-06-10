package soko.ekibun.bangumi.plugins.scripts.dmzj

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "dmzj",
        color = 0x198be3,
        title = "动漫之家",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "摇曳百合"
    override val lineInfo = LineInfo(
        "dmzj",
        id = "7020",
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