var doc = Jsoup.parse(http.fetch("https://www.ohmanhua.com" + line.id).body().string())
var category = doc.select("a[lineId]").toArray().map(it => it.text())
var data = []
doc.select(".all_data_list").toArray().forEach((list, index) => {
    data = data.concat(list.select("li > a").toArray().map(it => {
        var sort = /(\d+)/.exec(it.text())
        return {
            site: "ohmanhua",
            id: it.attr("href"),
            category: category[index],
            sort: sort && sort[1] || 0,
            title: it.text(),
            url: "https://www.ohmanhua.com" + it.attr("href")
        }
    }).reverse())
})
return data