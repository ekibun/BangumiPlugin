package soko.ekibun.bangumi.plugins.provider

class VideoProvider(
    search: String,
    @Provider.Code("获取剧集信息", 1) val getVideoInfo: String = "",  // (line: LineInfo, episode: VideoEpisode) -> VideoInfo
    @Provider.Code("获取视频信息", 2) val getVideo: String = "",      // (video: VideoInfo) -> VideoRequest
    @Provider.Code("获取弹幕信息", 3) val getDanmakuKey: String = "", // (video: VideoInfo) -> String
    @Provider.Code("获取弹幕", 4) val getDanmaku: String = ""     // (video: VideoInfo, key: String, pos: Int) -> List<DanmakuInfo>
): Provider(search)