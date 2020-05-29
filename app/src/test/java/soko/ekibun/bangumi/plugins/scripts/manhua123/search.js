var doc = Jsoup.parse(http.fetch("https://m.manhua123.net/index.php?m=vod-search-wd-"+key + ".html").body().string());
return doc.select("li.vbox a.vbox_t").toArray().map(it => {
    var id = /comic\/(\d+)/.exec(it.attr("href"))[1];
    return {
        site: "manhua123",
        id: id,
        title: it.attr("title")
    }
});