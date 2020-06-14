package soko.ekibun.bangumi.plugins.scripts.ohmanhua

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.book.BookProvider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "ohmanhua",
        color = 0x009fe9,
        title = "Oh漫画",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "斗罗大陆"
    override val lineInfo = LineInfo(
        "ohmanhua",
        id = "/10001/",
        extra = ""
    )
    override val episode = BookProvider.BookEpisode(
        site = "ohmanhua",
        id = "/10001/1/1.html",
        sort = 1f,
        title = "序章",
        url = "https://www.ohmanhua.com/10001/1/1.html"
    )
}