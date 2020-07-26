package soko.ekibun.bangumi.plugins.scripts.video.pptv

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
        site = "pptv",
        color = 0x009ee2,
        title = "PP视频",
        type = Provider.TYPE_VIDEO
    )
    override val searchKey = "绝望先生"
    override val lineInfo = LineInfo(
        "pptv",
        id = "10030280",
        title = "再见!绝望先生"
    )
    override val video = VideoProvider.VideoInfo(
        site = "iqiyi",
        id = "11700831",
        url = "http://v.pptv.com/show/XxMEgrIkLWvOTLQ.html?rcc_src\u003dB3"
    )
}