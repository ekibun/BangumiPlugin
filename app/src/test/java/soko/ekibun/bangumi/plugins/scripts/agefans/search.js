var url = "https://www.agefans.tv/search?query=";
var doc = Jsoup.parse(http.fetch(url+key).body().string());
return doc.select(".blockcontent1 a.cell_imform_name").toArray().map(it => {
    var url = it.attr("href");
    return {
        site: "agefans",
        id: /\/([0-9]+)$/g.exec(url)[1]+"?playid=2",
        title: it.text()
    };
});