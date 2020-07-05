var doc = Jsoup.parse(http.fetch(line.id).body().string())
return doc.select('table.TB2 > a').toArray().map((it, i, arr) => ({
    site: "gamertw",
    id: it.attr("href"),
    category: v.title,
    sort: arr.length - i,
    title: it.text(),
    url: "https://home.gamer.com.tw/" + it.attr("href")
})).filter(it => it.id.contains('creationDetail')).reverse()