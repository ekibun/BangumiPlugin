var url = "http://search.pptv.com/s_video?&type=3&kw="+key;
var doc = Jsoup.parse(http.fetch(url).body().string());
return doc.select(".content-box > .positive-box > a").toArray().map(a => {
    var it = {}
    try { it = JSON.parse(a.attr("ext_info").replace(/'/g, '"')) } catch (e) {  };
    return {
        site: "pptv",
        id: it.video_id,
        title: it.modulename
    };
});