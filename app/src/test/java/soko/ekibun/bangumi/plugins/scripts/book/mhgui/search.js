var doc = Jsoup.parse(http.fetch("https://m.mhgui.com/s/" + key + ".html").body().string())
return doc.select("#detail li > a").toArray().map(it => {
    var id = /comic\/([^/]+)/.exec(it.attr("href"))[1];
    return {
        site: "mhgui",
        id: id,
        title: it.selectFirst("h3").text()
    }
});