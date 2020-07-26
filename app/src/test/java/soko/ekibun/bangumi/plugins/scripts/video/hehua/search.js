var url = "https://www.hehua.net/mv/search/?wd=" + key;
var doc = Jsoup.parse(http.fetch(url).body().string());
return doc.select(".publicbox-list li > a").toArray().map(it => {
    return {
        site: "hehua",
        id: it.attr("href") + "1",
        title: it.text()
    };
});