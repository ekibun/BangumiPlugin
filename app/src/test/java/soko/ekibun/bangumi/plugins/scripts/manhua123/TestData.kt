package soko.ekibun.bangumi.plugins.scripts.manhua123

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.book.BookProvider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "manhua123",
        color = 0xff6880,
        title = "漫画123",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "摇曳百合"
    override val lineInfo = LineInfo(
        "manhua123",
        id = "10848",
        title = "大室家摇曳百合外传",
        extra = ""
    )
    override val episode = BookProvider.BookEpisode(
        site = "manhua123",
        id = "/comic/28093/1451941.html",
        sort = 52f,
        title = "第52话",
        url = "https://m.manhua123.net/comic/28093/1451941.html"
    )
}