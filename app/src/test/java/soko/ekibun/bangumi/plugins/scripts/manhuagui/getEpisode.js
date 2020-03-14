var doc = Jsoup.parse(http.get("https://m.manhuagui.com"+line.id).body().string());
return doc.select("div.chapter > div.chapter-list ul li a").toArray().map(it => {
    var id = /\/(\d+).html/.exec(it.attr("href"))[1];
    var sort = /(\d+)/.exec(it.text())
    return {
        site: "manhuagui",
        id: id,
        sort: sort && sort[1] || 0,
        title: it.text(),
        url: "https://m.manhuagui.com"+line.id+id+".html"
    }
}).reverse();