package soko.ekibun.bangumi.plugins.scripts.book.mh177

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "mh177",
        color = 0xff8207,
        title = "新新漫画",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "刺客守则"
    override val lineInfo = LineInfo(
        "mh177",
        id = "240282",
        title = "刺客守则",
        extra = ""
    )
    override val episode = Provider.ProviderEpisode(
        site = "mh177",
        id = "/201707/362631.html",
        sort = 1f,
        title = "刺客守则 第1话",
        url = "https://www.177mh.net/201707/362631.html"
    )
}