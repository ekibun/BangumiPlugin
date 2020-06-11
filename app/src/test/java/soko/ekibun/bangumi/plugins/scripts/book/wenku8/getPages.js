var doc = Jsoup.parse(http.inflate(http.fetch(episode.url).body().bytes(), "gb2312"))
var content = doc.selectFirst("#content");
content.select("#contentdp").remove()
var novel = content.wholeText()
novel = novel.trim() && [{
    content: novel
}] || [];
return novel.concat(content.select("img").toArray().map((it, index) => ({
    image: {
        url: it.attr("src"),
        header: {
            referer: ""
        }
    }
})))