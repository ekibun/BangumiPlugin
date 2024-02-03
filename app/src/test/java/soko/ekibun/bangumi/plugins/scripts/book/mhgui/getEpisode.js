var doc = Jsoup.parse(http.fetch("https://m.mhgui.com/comic/" + line.id + "/").body().string())
return doc.select("#chapterList li > a").toArray().map(it => {
    var id = /\/(\d+).html/.exec(it.attr("href"))[1];
    var sort = /(\d+)/.exec(it.text())
    return {
        site: "mhgui",
        id: id,
        sort: sort && sort[1] || 0,
        title: it.text(),
        url: "https://m.mhgui.com/comic/"+line.id+"/"+id+".html"
    }
}).reverse();