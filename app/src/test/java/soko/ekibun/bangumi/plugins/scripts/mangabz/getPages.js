var data = []
function getImage(page) {
    var doc = http.get("http://www.mangabz.com/m" + episode.id + "/chapterimage.ashx?cid=" + episode.id + "&page=" + page, {
        "referer": String("http://www.mangabz.com/m" + episode.id + "/")
    }).body().string()
    var pageData = eval(doc)
    java.lang.System.out.println(JSON.stringify(pageData))
    if(pageData[0] == data[data.length - 1]) return data
    data = data.concat(pageData)
    return getImage(data.length + 1)
}

return getImage(1).map(it => ({
    image: {
        url: it
    }
}))