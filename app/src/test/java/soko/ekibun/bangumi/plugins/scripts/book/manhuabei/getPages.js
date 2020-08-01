var doc = Jsoup.parse(http.fetch(episode.url).body().string());
var decrypt = doc.select("script").toArray().find(it => it.attr("src").startsWith("/js/decrypt")).attr("src");
eval(http.fetch("https://m.manhuabei.com" + decrypt).body().string());
var script = doc.select("script").toArray().find(it => it.html().includes("chapterImages")).html();
eval(script);
script = doc.select("script").toArray().find(it => it.html().includes("decrypt")).html().split(';');
try { eval(script.find(it => it.includes("decrypt"))) } catch(e) {};
return chapterImages.map(item => ({
    image: {
        url: item.startsWith("http")? item : "https://mhcdn.manhuazj.com/" + chapterPath + item,
        header: {
            referer: item.includes("dmzj")? "https://m.dmzj.com/" : "https://m.manhuabei.com/"
        }
    }
}))