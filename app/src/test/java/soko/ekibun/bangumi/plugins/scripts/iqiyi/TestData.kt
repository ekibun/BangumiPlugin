package soko.ekibun.bangumi.plugins.scripts.iqiyi

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
        site = "iqiyi",
        color = 0x00be06,
        title = "爱奇艺",
        type = Provider.TYPE_VIDEO
    )
    override val searchKey = "柯南"
    override val lineInfo = LineInfo(
        "iqiyi",
        id = "202134201",
        title = "名侦探柯南"
    )
    override val video = VideoProvider.VideoInfo(
        site = "iqiyi",
        id = "302725300",
        url = "http://www.iqiyi.com/v_19rrnfnjyw.html"
    )
}