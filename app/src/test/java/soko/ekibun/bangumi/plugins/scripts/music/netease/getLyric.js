var json = JSON.parse(fetch("https://music.163.com/weapi/song/lyric?csrf_token=", {
    id: episode.id,
    tv: -1,
    lv: -1,
    csrf_token: ""
}).body().string())
return {
    lrc: json.lrc.lyric,
    tlyric: json.tlyric && json.tlyric.lyric
}