function getDanmaku(page){
    var url = "http://ppgw.suning.com/absp/barrage/vod/query?systemId=PPTV&channelId="+video.id+"&pos="+ (page* 1000) +"&offset=100&ssgw-channel=pptv&callback=";
    var doc = JSON.parse(http.fetch(url).body().string());
    return doc.data.rows.map(it=>{
        return {
            time: Number(it.pts),
            type: 1,
            textSize: 25,
            color: Number(it.params.color.replace("#", '0x')) || 0xffffff,
            content: it.params.text
        };
    });
}
var pageStart = Math.floor(pos / 300) * 3;
var ret = [];
for(var i = 0; i < 5; i++){
    ret = ret.concat(getDanmaku(pageStart + i));
}
return ret;