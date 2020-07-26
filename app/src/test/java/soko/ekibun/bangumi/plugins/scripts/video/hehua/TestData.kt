package soko.ekibun.bangumi.plugins.scripts.video.hehua

import soko.ekibun.bangumi.plugins.VideoScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : VideoScriptTest.VideoTestData() {
    /**
     * 线路配置
     */
    override val info = ProviderInfo(
        site = "hehua",
        color = 0x7332d6,
        title = "荷花网",
        type = Provider.TYPE_VIDEO
    )
    override val searchKey = "绝望先生"
    override val lineInfo = LineInfo(
        "fodm",
        id = "/mv/dongman/zaijianjuewangxiansheng2/2",
        title = "再见，绝望先生"
    )
}