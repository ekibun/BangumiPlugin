var doc = Jsoup.parse(http.fetch("http://www.nicotv.me/video/search/"+key).body().string());
return doc.select("ul.vod-item-img p.image> a").toArray().map(it => {
    var url = it.attr("href");
    return {
        site: "nicotv",
        id: /\/([0-9]+).html/g.exec(url)[1]+"-1",
        title: it.selectFirst("img").attr("alt")
    };
});