package soko.ekibun.bangumi.plugins.scripts.manhuagui

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.book.BookProvider

class TestData : BookScriptTest.BookTestData() {
    override val info = LineProvider.ProviderInfo(
        site = "manhuagui",
        color = 0x0791ff,
        title = "漫画柜",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "刺客守则"
    override val lineInfo = LineInfoModel.LineInfo(
        "manhuagui",
        id = "/comic/24086/",
        title = "刺客守则",
        extra = ""
    )
    override val episode = BookProvider.BookEpisode(
        site = "manhuagui",
        id = "304069",
        sort = 1f,
        title = "第01回",
        url = "https://m.manhuagui.com/comic/24086/304069.html"
    )
}