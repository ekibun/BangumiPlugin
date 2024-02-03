var doc = Jsoup.parse(http.fetch("https://www.manhuadb.com/search?q="+key).body().string());
return doc.select(".comicbook-index > a").toArray().map(it => {
    var id = /manhua\/([^/]+)/.exec(it.attr("href"))[1];
    return {
        site: "manhuadb",
        id: id,
        title: it.attr("title")
    }
});