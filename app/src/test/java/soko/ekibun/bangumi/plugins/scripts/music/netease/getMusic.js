var json = JSON.parse(fetch("http://music.163.com/weapi/song/enhance/player/url?csrf_token=", {
    ids: [episode.id],
    br: 320000,
    csrf_token: ""
}).body().string())
return {
    url: json.data[0].url
}