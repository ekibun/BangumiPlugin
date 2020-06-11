package soko.ekibun.bangumi.plugins.scripts.video.nicotv

import soko.ekibun.bangumi.plugins.VideoScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : VideoScriptTest.VideoTestData() {
    /**
     * 线路配置
     */
    override val info = ProviderInfo(
        site = "nicotv",
        color = 0x666666,
        title = "nicotv",
        type = Provider.TYPE_VIDEO
    )
    override val searchKey = "日常"
    override val lineInfo = LineInfo(
        "nicotv",
        id = "55188-1",
        title = "魔物娘的相伴日常（无修）"
    )
}