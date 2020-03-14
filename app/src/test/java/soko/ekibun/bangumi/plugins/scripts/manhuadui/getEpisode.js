var doc = Jsoup.parse(http.get("https://m.manhuadui.com/manhua/"+line.id+"/").body().string());
return doc.select("#list_block li > a").toArray().map(it => {
    var id = /\/(\d+).html/.exec(it.attr("href"))[1];
    var sort = /(\d+)/.exec(it.text())
    return {
        site: "manhuadui",
        id: id,
        sort: sort && sort[1] || 0,
        title: it.text(),
        url: "https://m.manhuadui.com/manhua/"+line.id+"/"+id+".html"
    }
});