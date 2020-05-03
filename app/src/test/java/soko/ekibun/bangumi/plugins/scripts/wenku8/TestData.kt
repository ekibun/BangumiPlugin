package soko.ekibun.bangumi.plugins.scripts.wenku8

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.book.BookProvider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "wenku8",
        color = 0x7597e7,
        title = "轻小说文库",
        type = Provider.TYPE_BOOK
    )
    override val searchKey = "刺客守则"
    override val lineInfo = LineInfo(
        "test",
        id = "2159",
        extra = "",
        subjectId = 0
    )
    override val episode = BookProvider.BookEpisode(
        site = "wenku8",
        id = "78134.htm",
        sort = 1f,
        category = "第一卷 暗杀教师与无能才女",
        title = "HOMEROOM EARLIER",
        url = "https://www.wenku8.net/novel/2/2159/78134.htm"
    )
}