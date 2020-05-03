package soko.ekibun.bangumi.plugins.scripts.manhuadui

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.book.BookProvider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "manhuadui",
        color = 0x31a4fd,
        title = "漫画堆",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "摇曳百合"
    override val lineInfo = LineInfo(
        "manhuadui",
        id = "yaoyebaihe",
        title = "摇曳百合",
        extra = "",
        subjectId = 0
    )
    override val episode = BookProvider.BookEpisode(
        site = "manhuadui",
        id = "279908",
        sort = 1f,
        title = "01话",
        url = "https://m.manhuadui.com/manhua/cikeshouze/279908.html"
    )
}