var data = JSON.parse(http.fetch("https://www.pixiv.net/ajax/illust/" + episode.id + "/pages").body().string())
return data.body.map(it => ({
    image: {
        url: it.urls.original,
    }
}))