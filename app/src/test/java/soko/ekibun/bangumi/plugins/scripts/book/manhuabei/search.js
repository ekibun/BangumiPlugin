var doc = Jsoup.parse(http.fetch(host+"/search/?keywords="+key).body().string());
return doc.select("#update_list a.title").toArray().map(it => {
    var id = /manhua\/([^/]+)/.exec(it.attr("href"))[1];
    return {
        site: "manhuabei",
        id: id,
        title: it.text()
    }
});