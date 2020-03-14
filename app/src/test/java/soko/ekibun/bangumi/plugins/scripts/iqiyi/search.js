var url = "https://search.video.iqiyi.com/o?channel_name=动漫&if=html5&key="+key+"&pageSize=20&video_allow_3rd=0";
var json = JSON.parse(http.get(url).body().string());
return json.data.docinfos.map(prn => {
    var it = prn.albumDocInfo;
    if(!it.score || it.siteId != "iqiyi") return null;
    return {
        site: "iqiyi",
        id: it.albumId,
        title: it.albumTitle
    };
}).filter(it => it);