package soko.ekibun.bangumi.plugins.scripts.nicotv

import soko.ekibun.bangumi.plugins.VideoScriptTest
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : VideoScriptTest.VideoTestData() {
    /**
     * 线路配置
     */
    override val info = LineProvider.ProviderInfo(
        site = "nicotv",
        color = 0x666666,
        title = "nicotv",
        type = Provider.TYPE_VIDEO
    )
    override val searchKey = "日常"
    override val lineInfo = LineInfoModel.LineInfo(
        "nicotv",
        id = "55188-1",
        title = "魔物娘的相伴日常（无修）"
    )
}