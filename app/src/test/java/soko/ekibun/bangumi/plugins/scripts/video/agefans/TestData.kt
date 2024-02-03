package soko.ekibun.bangumi.plugins.scripts.video.agefans

import soko.ekibun.bangumi.plugins.VideoScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : VideoScriptTest.VideoTestData() {
    /**
     * 线路配置
     */
    override val info = ProviderInfo(
        site = "agefans",
        color = 0x292929,
        title = "agefans",
        type = Provider.TYPE_VIDEO
    )
    override val searchKey = "轻音少女"
    override val lineInfo = LineInfo(
        "agefans",
        id = "20090010/1",
        title = "轻音少女"
    )
}