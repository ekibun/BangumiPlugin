var url = "https://www.agedm.org/search?query=";
var doc = Jsoup.parse(http.fetch(url+key).body().string());
return doc.select(".card-title > a").toArray().map(it => {
    var url = it.attr("href");
    return {
        site: "agefans",
        id: /\/([0-9]+)$/g.exec(url)[1]+"/1",
        title: it.text()
    };
});