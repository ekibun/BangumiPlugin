var ep_sort = episode.sort + Number(line.extra||0);
var doc = http.fetch("https://m.acfun.cn/v/?ab="+line.id).body().string();
var data = JSON.parse(/videoInfo *?= ?(.*?);/.exec(doc)[1]);
var ep = data.group[(ep_sort - 1) | 0];
return {
    site: "acfun",
    id: ep.videoId,
    url: "http://www.acfun.cn/bangumi/aa"+ep.bangumiId+"_"+36188+"_"+ep.itemId
};