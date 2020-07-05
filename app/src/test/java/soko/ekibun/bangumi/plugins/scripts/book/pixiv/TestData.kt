package soko.ekibun.bangumi.plugins.scripts.book.pixiv

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "pixiv",
        color = 0x0096fa,
        title = "pixiv",
        type = Provider.TYPE_BOOK
    )
    override val lineInfo = LineInfo(
        "pixiv",
        id = "{\"user\": 159912, \"tag\": \"お兄ちゃんはおしまい\"}",
        extra = ""
    )
    override val episode = Provider.ProviderEpisode(
        site = "pixiv",
        id = "82589472",
        sort = 1f,
        title = "第01话",
        url = "https://m.dmzj.com/view/40523/59967.html"
    )
}