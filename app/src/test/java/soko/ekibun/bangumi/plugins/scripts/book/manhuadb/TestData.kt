package soko.ekibun.bangumi.plugins.scripts.book.manhuadb

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "manhuadb",
        color = 0xe40b21,
        title = "漫画DB",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "摇曳百合"
    override val lineInfo = LineInfo(
        "manhuadb",
        id = "5315",
        title = "大室家",
        extra = ""
    )
    override val episode = Provider.ProviderEpisode(
        site = "manhuadb",
        id = "6673_105181",
        sort = 1f,
        title = "第01卷",
        url = "https://www.manhuadb.com/manhua/5315/6673_105181.html"
    )
}