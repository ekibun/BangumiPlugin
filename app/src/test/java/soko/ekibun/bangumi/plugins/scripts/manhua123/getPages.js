var doc = Jsoup.parse(http.get(episode.url).body().string());
var script = doc.select("script").toArray().find(it => it.html().includes("z_img")).html()
eval(script)

return eval(z_img).map(it => ({
    image: {
        url: z_yurl + it,
        header: {
            referer: ""
        }
    }
}))