var getDanmaku = async((page)=>{
    var url = "https://mfm.video.qq.com/danmu?timestamp="+(page*30)+"&target_id="+key;
    var json = JSON.parse(http.get(url).body().string());
    return json.comments.map(it=>{
        var contentStyle = it.content_style && JSON.parse(it.content_style);
        return {
            time: Number(it.timepoint),
            type: contentStyle.contentStyle == null ? 1 : 5,
            textSize: 25,
            color: Number("0x"+contentStyle.color) || 0xffffff,
            content: it.content
        };
    });
});
var pageStart = Math.floor(pos / 300) * 10;
var tasks = [];
for(var i = 0; i < 20; i++){
    tasks.push(getDanmaku(pageStart + i));
}
var ret = [];
for(var i in tasks){
    try{ ret = ret.concat(await(tasks[i])) } catch(e){}
}
return ret;