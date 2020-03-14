var url = "https://comment.bilibili.com/"+video.id+".xml";
var doc = Jsoup.parse(http.inflate(http.get(url).body().bytes(), "deflate"));
return doc.select("d").toArray().map(it => {
    var p = it.attr("p").split(",");
    return {
        time: Number(p[0]),
        type: Number(p[1]),
        textSize: Number(p[2]),
        color: Number(p[3]),
        content: it.text(),
        timeStamp: Number(p[4])
    };
});