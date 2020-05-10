package soko.ekibun.bangumi.plugins.scripts.mangabz

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.book.BookProvider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "mangabz",
        color = 0xff4a32,
        title = "Māngabz",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "鬼灭之刃"
    override val lineInfo = LineInfo(
        "mangabz",
        id = "/73bz/",
        title = "鬼滅之刃",
        extra = ""
    )
    override val episode = BookProvider.BookEpisode(
        site = "mangabz",
        id = "10344",
        sort = 1f,
        title = "1",
        url = "http://www.mangabz.com/m10344/"
    )
}