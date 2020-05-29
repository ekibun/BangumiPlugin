package soko.ekibun.bangumi.plugins.scripts.acfun

import soko.ekibun.bangumi.plugins.VideoScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.video.VideoProvider

class TestData : VideoScriptTest.VideoTestData() {
    /**
     * 线路配置
     */
    override val info = ProviderInfo(
        site = "acfun",
        color = 0xfd4c5b,
        title = "Acfun",
        type = Provider.TYPE_VIDEO
    )

    /**
     * 测试用的数据
     */
    override val searchKey = "隐瞒之事"
    override val lineInfo = LineInfo(
        "acfun",
        id = "6001745",
        title = "隐瞒之事"
    )
    override val video = VideoProvider.VideoInfo(
        site = "acfun",
        id = "12873383",
        url = "http://www.acfun.cn/bangumi/aa6001745_36188_1726446"
    )
}