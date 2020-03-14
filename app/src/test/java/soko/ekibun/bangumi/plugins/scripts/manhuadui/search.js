var doc = Jsoup.parse(http.get("https://m.manhuadui.com/search/?keywords="+key).body().string());
return doc.select("#update_list a.title").toArray().map(it => {
    var id = /manhua\/([^/]+)/.exec(it.attr("href"))[1];
    return {
        site: "manhuadui",
        id: id,
        title: it.text()
    }
});