var doc = Jsoup.parse(http.fetch(episode.url).body().string());
var script = doc.select("script").toArray().find(it => it.html().includes("z_img")).html()
eval(script)

return eval(z_img).map(it => ({
    image: {
        url: "https://img.detatu.com/" + it,
        header: {
            referer: ""
        }
    }
}))