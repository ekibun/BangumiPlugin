package soko.ekibun.bangumi.plugins.scripts.book.manhuabei

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "manhuabei",
        color = 0x31a4fd,
        title = "漫画呗",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "摇曳百合"
    override val lineInfo = LineInfo(
        "manhuabei",
        id = "yaoyebaihe",
        title = "摇曳百合",
        extra = ""
    )
    override val episode = Provider.ProviderEpisode(
        site = "manhuabei",
        id = "279908",
        sort = 1f,
        title = "01话",
        url = "http://wap.ykmh.com/manhua/yaoyebaihe/65427.html"
    )
}