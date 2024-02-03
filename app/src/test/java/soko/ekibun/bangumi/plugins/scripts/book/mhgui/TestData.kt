package soko.ekibun.bangumi.plugins.scripts.book.mhgui

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "mhgui",
        color = 0x0791ff,
        title = "漫画柜",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "摇曳百合"
    override val lineInfo = LineInfo(
        "mhgui",
        id = "6414",
        extra = ""
    )
    override val episode = Provider.ProviderEpisode(
        site = "mhgui",
        id = "55779",
        sort = 1f,
        title = "第01话",
        url = "https://m.mhgui.com/comic/6414/55779.html"
    )
}