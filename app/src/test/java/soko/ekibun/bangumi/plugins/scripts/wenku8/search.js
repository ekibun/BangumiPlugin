var rsp = http.get("https://www.wenku8.net/modules/article/search.php?searchtype=articlename&searchkey=" + java.net.URLEncoder.encode(key, "gb2312"), header)
var doc = Jsoup.parse(http.inflate(rsp.body().bytes(), "gb2312"));
if(rsp.priorResponse()) return [{
    site: "wenku8",
    id: /book\/(\d+).htm/.exec(rsp.priorResponse().headers().get("Location"))[1],
    title: doc.selectFirst("#content b").text()
}]
return doc.select("table.grid b > a").toArray().map(it => {
    var id = /book\/(\d+).htm/.exec(it.attr("href"))[1];
    return {
        site: "wenku8",
        id: id,
        title: it.text()
    }
})