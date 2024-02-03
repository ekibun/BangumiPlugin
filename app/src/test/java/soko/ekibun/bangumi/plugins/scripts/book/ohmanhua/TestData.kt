package soko.ekibun.bangumi.plugins.scripts.book.ohmanhua

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "ohmanhua",
        color = 0x009fe9,
        title = "Oh漫画",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "一人之下"
    override val lineInfo = LineInfo(
        "ohmanhua",
        id = "/10263/",
        title = "一人之下",
        extra = ""
    )
    override val episode = Provider.ProviderEpisode(
        site = "ohmanhua",
        id = "/10263/1/1.html",
        sort = 1f,
        title = "1.姐姐1",
        url = "https://www.cocomanhua.com/10263/1/1.html"
    )
}