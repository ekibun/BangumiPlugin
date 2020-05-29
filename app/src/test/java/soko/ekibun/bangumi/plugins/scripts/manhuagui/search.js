var doc = Jsoup.parse(http.fetch("https://m.manhuagui.com/s/" + key + ".html").body().string());
return doc.select("#detail li > a").toArray().map(it => {
    it.selec
    return {
        site: "manhuagui",
        id: it.attr("href"),
        title: it.selectFirst("h3").text()
    }
});