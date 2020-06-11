var json = JSON.parse(fetch("http://music.163.com/weapi/v1/album/" + line.id + "?csrf_token=", {
    album: line.id,
    csrf_token: ""
}).body().string())
return json.songs.map((it, index) => ({
    site: "netease",
    id: it.id,
    sort: index + 1,
    title: it.name,
    url: "https://music.163.com/#/song?id=" + it.id
}))