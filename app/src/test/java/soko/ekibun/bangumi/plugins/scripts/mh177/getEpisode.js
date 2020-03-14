var doc = Jsoup.parse(http.get("https://www.177mh.net/colist_"+line.id+".html").body().string());
return doc.select(".ar_list_col li>a").toArray().map(it => {
    var sort = /(\d+)/.exec(it.text())
    return {
        site: "mh177",
        id: it.attr("href"),
        sort: sort && sort[1] || 0,
        title: it.attr("title"),
        url: "https://www.177mh.net" + it.attr("href")
    }
}).reverse();