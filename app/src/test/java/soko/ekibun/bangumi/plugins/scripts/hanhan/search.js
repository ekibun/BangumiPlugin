var doc = Jsoup.parse(http.fetch(HANHAN_HOST + "/comic/?act=search&st=" + key).body().string());
return doc.select("#list > div.cComicList > li > a").toArray().map(it => {
    var id = /manhua\/(\d+).html/.exec(it.attr("href"))[1];
    return {
        site: site,
        id: id,
        title: it.text()
    }
})