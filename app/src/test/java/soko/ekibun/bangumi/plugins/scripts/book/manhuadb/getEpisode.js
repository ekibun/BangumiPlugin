var doc = Jsoup.parse(http.fetch("https://www.manhuadb.com/manhua/"+line.id).body().string());
var data = [];
doc.select("#myTab a.nav-link").toArray().forEach(chap => {
    var cat = chap.text();
    var lastIndex = 0;
    data = data.concat(doc.select(chap.attr("href") + " .links-of-books a").toArray().map(it => {
        var id = /\/([0-9_]+).html/.exec(it.attr("href"))[1];
        var sort = /(\d+)/.exec(it.text())
        sort = sort && sort[1] || 0
        lastIndex = Math.max(lastIndex, sort);
        return {
            site: "manhuadb",
            id: id,
            sort: lastIndex,
            category: cat,
            title: it.text(),
            url: "https://www.manhuadb.com/manhua/"+line.id+"/"+id+".html"
        }
    }))
})
return data;