var doc = Jsoup.parse(http.fetch("https://www.cocomanhua.com/search?searchString=" + key).body().string())
return doc.select(".fed-deta-info.fed-deta-padding.fed-line-top.fed-margin.fed-part-rows.fed-part-over h1>a").toArray().map(it => ({
    site: "ohmanhua",
    id: it.attr("href"),
    title: it.text()
}))