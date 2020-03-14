var url = "https://www.wenku8.net/novel/" + parseInt(line.id / 1000) + "/" + line.id + "/"
var doc = Jsoup.parse(http.inflate(http.get(url + "index.htm").body().bytes(), "gb2312"))
var result = []
var cat = ""
var index = 0
doc.select("td").toArray().forEach((v) => {
    if(v.hasClass("vcss")){
        cat = v.text()
        index = 0
    }
    var a = v.selectFirst("a")
    if(a) result.push({
        site: "wenku8",
        id: a.attr("href"),
        sort: index,
        category: cat,
        title: a.text(),
        url: url + a.attr("href")
    })
    index++;
})
return result