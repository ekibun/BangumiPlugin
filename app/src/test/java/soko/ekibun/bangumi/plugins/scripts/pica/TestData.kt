package soko.ekibun.bangumi.plugins.scripts.pica

import soko.ekibun.bangumi.plugins.BookScriptTest
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : BookScriptTest.BookTestData() {
    override val info = ProviderInfo(
        site = "pica",
        color = 0xff8a98,
        title = "哔咔漫画",
        type = Provider.TYPE_BOOK
    )
}