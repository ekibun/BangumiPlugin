package soko.ekibun.bangumi.plugins.scripts.acfun

import soko.ekibun.bangumi.plugins.VideoScriptTest
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.video.VideoProvider
import soko.ekibun.bangumi.plugins.util.JsonUtil

class TestData : VideoScriptTest.VideoTestData {
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
        "test",
        id = "6000901"
    )
    override val episode = Episode(
        sort = 1f
    )
    override val video = VideoProvider.VideoInfo(
        site = "test",
        id = "11188351",
        url = "http://www.acfun.cn/bangumi/aa6000901_35425_1707941"
    )
    override val danmakuKey = JsonUtil.toJson("")
}