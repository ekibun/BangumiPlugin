package soko.ekibun.bangumi.plugins.scripts.hanhan

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.book.BookProvider
import soko.ekibun.bangumi.plugins.util.HttpUtil

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "hanhan",
        color = 0x003333,
        title = "汗汗酷漫",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "日常"
    override val lineInfo = LineInfo(
        "hanhan",
        id = "36056",
        title = "机械依存系少女的麻烦日常",
        extra = ""
    )
    override val episode = BookProvider.BookEpisode(
        site = "hanhan",
        id = "/cool323156/1.html?s=8",
        sort = 1f,
        title = "机械依存系少女 001集",
        url = "http://www.hhimm.com/cool323156/1.html?s=8"
    )
    override val page = BookProvider.PageInfo(
        image = HttpUtil.HttpRequest(
            url = "http://www.hhimm.com/cool376511/1.html?s=3"
        )
    )
}