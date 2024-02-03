var doc = Jsoup.parse(http.fetch(host+"/manhua/"+line.id+"/").body().string());
var data = [];
doc.select("#list_block .comic-chapters").toArray().forEach(chap => {
    var cat = chap.selectFirst(".caption span.Title").text();
    var lastIndex = 0;
    data = data.concat(chap.select(".chapter-warp li > a").toArray().map(it => {
        var id = /\/(\d+).html/.exec(it.attr("href"))[1];
        var sort = /(\d+)/.exec(it.text())
        sort = sort && sort[1] || 0
        lastIndex = Math.max(lastIndex, sort);
        return {
            site: "manhuabei",
            id: id,
            sort: lastIndex,
            category: cat,
            title: it.text(),
            url: host+"/manhua/"+line.id+"/"+id+".html"
        }
    }))
})
return data;