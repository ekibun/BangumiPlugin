var url = "http://tldm.net/search.asp";
var body = "searchword="+java.net.URLEncoder.encode(key, "gb2312")
var doc = Jsoup.parse(http.inflate(http.fetch(url,{
    body: "searchword="+java.net.URLEncoder.encode(key, "gb2312"),
    contentType: "application/x-www-form-urlencoded"
}).body().bytes(), "gb2312"));
return doc.select(".movie-chrList .cover> a").toArray().map(it => {
    return {
        site: "fodm",
        id: it.attr("href"),
        title: it.selectFirst("img").attr("alt")
    };
});