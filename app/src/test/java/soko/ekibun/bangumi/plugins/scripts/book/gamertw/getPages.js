var doc = Jsoup.parse(http.fetch(episode.url).body().string())
return doc.select("div.home_box img").toArray().map(it => ({
    image: {
        url: it.attr('data-src'),
    }
}))