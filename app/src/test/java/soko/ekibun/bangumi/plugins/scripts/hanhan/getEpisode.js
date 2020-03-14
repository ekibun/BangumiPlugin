var doc = Jsoup.parse(http.get(HANHAN_HOST + "/manhua/"+line.id+".html").body().string());
return doc.select("#permalink > div.cVolList > ul.cVolUl > li > a").toArray().map(it => {
    var title = it.text()
    var sort = /(\d+)/.exec(title.split(" ")[1] || title)
    return {
        site: site,
        id: it.attr("href"),
        sort: sort && sort[1] || 0,
        title: title,
        url: HANHAN_HOST + it.attr("href")
    }
}).reverse()