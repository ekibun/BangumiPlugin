package soko.ekibun.bangumi.plugins.scripts.dmzj

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.book.BookProvider

class TestData : BookScriptTest.BookTestData() {
    override val info = LineProvider.ProviderInfo(
        site = "dmzj",
        color = 0x198be3,
        title = "动漫之家",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "刺客守则"
    override val lineInfo = LineInfoModel.LineInfo(
        "dmzj",
        id = "40523",
        extra = ""
    )
    override val episode = BookProvider.BookEpisode(
        site = "dmzj",
        id = "59967",
        sort = 1f,
        title = "第01话",
        url = "https://m.dmzj.com/view/40523/59967.html"
    )
}