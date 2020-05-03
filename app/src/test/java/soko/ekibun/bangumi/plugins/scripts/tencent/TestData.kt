package soko.ekibun.bangumi.plugins.scripts.tencent

import soko.ekibun.bangumi.plugins.VideoScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.video.VideoProvider
import soko.ekibun.bangumi.plugins.util.JsonUtil

class TestData : VideoScriptTest.VideoTestData() {
    /**
     * 线路配置
     */
    override val info = ProviderInfo(
        site = "tencent",
        color = 0xff820f,
        title = "腾讯视频",
        type = Provider.TYPE_VIDEO
    )
    override val searchKey = "柯南"
    override val lineInfo = LineInfo(
        "tencent",
        id = "ejq8xk8z9lni3q2",
        title = "[电影] 名侦探柯南：绀青之拳",
        subjectId = 0
    )
    override val video = VideoProvider.VideoInfo(
        site = "tencent",
        id = "h0032gdjr4q",
        url = "https://v.qq.com/x/cover/ejq8xk8z9lni3q2/h0032gdjr4q.html/h0032gdjr4q.html"
    )
    override val danmakuKey = JsonUtil.toJson("4351976498")
}