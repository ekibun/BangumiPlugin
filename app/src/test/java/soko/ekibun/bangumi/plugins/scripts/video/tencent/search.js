var url = "http://m.v.qq.com/search.html?keyWord="+key;
var doc = Jsoup.parse(http.fetch(url).body().string());
return doc.select(".search_item").toArray().map(it => {
    try{
        if(it.selectFirst(".mask_scroe") == null || !it.selectFirst(".figure_source").text().includes("腾讯")) return null;
        var genre = it.selectFirst(".figure_genre").text()
        var title = it.selectFirst(".figure_title").text()
        var url = it.selectFirst(".figure").attr("href")
        var vid = /\/([^/.]+).html/g.exec(url)[1];
        return {
            site: "tencent",
            id: vid,
            title: Jsoup.parse(title).text()
        };
    }catch(e){ return null; }
}).filter(it => it)