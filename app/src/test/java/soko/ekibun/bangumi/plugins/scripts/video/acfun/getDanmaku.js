var arr = [];
function getDanmaku(pos){
    var json=JSON.parse(http.fetch("https://m.acfun.cn/rest/mobile-direct/new-danmaku/poll", {
        headers: { cookie: "_did=web;" },
        body: {
            videoId: video.id,
            lastFetchTime: pos.toString()
        }
    }).body().string());
    var list = json.added.map(v => {
        return {
            time: v.position/1000,
            type: v.mode,
            textSize: v.size,
            color: v.color,
            content: v.body,
            timeStamp: v.danmakuId
        };
    });
    arr = arr.concat(list);
    if(list.length > 0) getDanmaku(json.fetchTime);
}
getDanmaku(0);
return arr;
