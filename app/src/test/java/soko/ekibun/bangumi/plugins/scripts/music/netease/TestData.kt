package soko.ekibun.bangumi.plugins.scripts.music.netease

import soko.ekibun.bangumi.plugins.MusicScriptTest
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider

class TestData : MusicScriptTest.MusicTestData() {
    /**
     * 线路配置
     */
    override val info = ProviderInfo(
        site = "netease",
        color = 0xe60026,
        title = "网易云音乐",
        type = Provider.TYPE_MUSIC
    )

    /**
     * 测试用的数据
     */
    override val searchKey = "DADDY!DADDY!DO!"
    override val lineInfo = LineInfo(
        site = "netease",
        id = "87925969",
        title = "DADDY! DADDY! DO! feat. 鈴木愛理"
    )
    override val episode = Provider.ProviderEpisode(
        site = "netease",
        id = "1440648326",
        sort = 1f,
        title = "DADDY! DADDY! DO!",
        url = "https://music.163.com/#/song?id=1440648326"
    )
}