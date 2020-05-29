function getDanmaku(page){
    var url = "http://cmts.iqiyi.com/bullet/"+video.id.substring(video.id.length-4,video.id.length-2)+"/"+video.id.substring(video.id.length-2,video.id.length)+"/"+video.id+"_300_"+page+".z";
    var doc = org.jsoup.Jsoup.parse(http.inflate(http.fetch(url).body().bytes(),"gzip"));
    return doc.select("bulletInfo").toArray().map(it=>{
        return {
            time: Number(it.selectFirst("showTime").text()),
            type: 1,
            textSize: 25,
            color: Number("0x"+it.selectFirst("color").text()) || 0xffffff,
            content: it.selectFirst("content").text()
        };
    });
}
var pageStart = Math.floor(pos / 300);
var ret = [];
for(var i = 0; i < 2; i++){
    ret = ret.concat(getDanmaku(pageStart + i));
}
return ret;