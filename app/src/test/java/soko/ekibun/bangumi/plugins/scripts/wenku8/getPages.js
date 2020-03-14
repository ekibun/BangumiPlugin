var doc = Jsoup.parse(http.inflate(http.get(episode.url).body().bytes(), "gb2312"))
var content = doc.selectFirst("#content");
content.select("#contentdp").remove()
var novel = content.html()
novel = novel.trim() && [{
    content: http.html2text(novel)
}] || [];
return novel.concat(content.select("img").toArray().map((it, index) => ({
    image: {
        url: it.attr("src")
    }
})))