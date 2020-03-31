var doc = Jsoup.parse(http.get("http://www.mangabz.com/search?title=" + key).body().string())
return doc.select(".manga-list a").toArray().map(it => ({
    site: "mangabz",
    id: it.attr("href"),
    title: it.selectFirst("p.manga-item-title").text()
}))