var cookie = App.load("wenku8_cookie")
if(!cookie) {
    var rsp = http.fetch("https://www.wenku8.net/login.php?do=submit", {
        body: {
            username: "",
            password: "",
            usecookie: "315360000",
            action: "login",
            submit: " 登 录 "
        }
    })
    cookie = rsp.headers("set-cookie").toArray().map((v) => v.split(';')[0]).join(";")
    App.dump("wenku8_cookie", cookie)
}

var rsp = http.fetch("https://www.wenku8.net/modules/article/search.php?searchtype=articlename&searchkey=" + java.net.URLEncoder.encode(key, "gb2312"), {
    headers: { cookie: cookie }
})
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