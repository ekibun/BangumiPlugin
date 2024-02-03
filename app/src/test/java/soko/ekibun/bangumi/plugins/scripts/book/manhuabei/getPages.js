var doc = Jsoup.parse(http.fetch(episode.url).body().string());
// var decrypt = doc.select("script").toArray().find(it => it.attr("src").startsWith("/js/decrypt")).attr("src");
// eval(http.fetch("https://m.manhuabei.com" + decrypt).body().string());
var script = doc.select("script").toArray().find(it => it.html().includes("chapterImages")).html();
eval(script);
// script = doc.select("script").toArray().find(it => it.html().includes("decrypt")).html();
// throw script;
return chapterImages.map(item => ({
    image: {
        url: item.startsWith("http")? item : "https://pic.w1fl.com" + item,
        header: {
            referer: item.includes("dmzj")? "https://m.dmzj.com/" : host+"/"
        }
    }
}))