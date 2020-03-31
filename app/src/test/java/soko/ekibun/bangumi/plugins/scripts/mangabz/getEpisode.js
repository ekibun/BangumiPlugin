var doc = Jsoup.parse(http.get("http://www.mangabz.com" + line.id).body().string())
return doc.select(".detail-list-item a").toArray().map(it => ({
    site: "mangabz",
    id: it.attr("href").replace(/[/m]/g, ""),
    sort: it.text() * 1,
    title: it.text(),
    url: "http://www.mangabz.com" + it.attr("href")
})).reverse();