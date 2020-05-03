package soko.ekibun.bangumi.plugins.scripts.ningmoe

import soko.ekibun.bangumi.plugins.VideoScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : VideoScriptTest.VideoTestData() {
    /**
     * 线路配置
     */
    override val info = ProviderInfo(
        site = "ningmoe",
        color = 0xff3e63,
        title = "柠萌",
        type = Provider.TYPE_VIDEO
    )
    override val searchKey = "日常"
    override val lineInfo = LineInfo(
        "ningmoe",
        id = "94624",
        title = "搞姬日常",
        subjectId = 0
    )
}