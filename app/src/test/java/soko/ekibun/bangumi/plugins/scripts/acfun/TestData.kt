package soko.ekibun.bangumi.plugins.scripts.acfun

import soko.ekibun.bangumi.plugins.VideoScriptTest
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.video.VideoProvider

class TestData : VideoScriptTest.VideoTestData() {
    /**
     * 线路配置
     */
    override val info = LineProvider.ProviderInfo(
        site = "acfun",
        color = 0xfd4c5b,
        title = "Acfun",
        type = Provider.TYPE_VIDEO
    )

    /**
     * 测试用的数据
     */
    override val searchKey = "房间露营"
    override val lineInfo = LineInfoModel.LineInfo(
        "acfun",
        id = "6000901",
        title = "房间露营"
    )
    override val video = VideoProvider.VideoInfo(
        site = "acfun",
        id = "11188351",
        url = "http://www.acfun.cn/bangumi/aa6000901_35425_1707941"
    )
}