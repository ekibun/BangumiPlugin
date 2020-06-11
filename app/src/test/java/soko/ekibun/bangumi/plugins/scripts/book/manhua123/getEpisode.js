var doc = Jsoup.parse(http.fetch("https://m.manhua123.net/comic/" + line.id + ".html").body().string())
return doc.select("ul.list_block a").toArray().map(it => {
    var sort = /(\d+)/.exec(it.text())
    return {
        site: "manhua123",
        id: it.attr("href"),
        sort: sort && sort[1] || 0,
        title: it.text(),
        url: "https://m.manhua123.net" + it.attr("href")
    }
});