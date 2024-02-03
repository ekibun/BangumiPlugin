var data = JSON.parse(http.fetch(episode.url.replace("/view/", "/chapinfo/")).body().string())
return data.page_url.map(it => ({
    image: {
        url: it,
        header: {
            referer: "https://m.dmzj.com/"
        }
    }
}))