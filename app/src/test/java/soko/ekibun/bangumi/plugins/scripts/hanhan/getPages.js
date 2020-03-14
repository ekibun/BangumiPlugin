var doc = Jsoup.parse(http.get(episode.url).body().string());
var pageCount = parseInt(doc.selectFirst("#hdPageCount").attr("value"));
var path = doc.selectFirst("#hdVolID").attr("value");
var hdS = doc.selectFirst("#hdS").attr("value");
return new Array(pageCount).join(0).split('').map((v,i) => ({
    site: site,
    image: {
        url: HANHAN_HOST + "/cool" + path + "/" + (i+1) + ".html?s=" + hdS
    }
}))