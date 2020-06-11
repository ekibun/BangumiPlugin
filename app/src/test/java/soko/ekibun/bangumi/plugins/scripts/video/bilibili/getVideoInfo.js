var ep_sort = episode.sort + Number(line.extra||0)
var url = "https://bangumi.bilibili.com/view/web_api/media?media_id=";
var json = JSON.parse(http.fetch(url+line.id).body().string());
var ep = json.result.episodes.find(it => Number(it.index) == ep_sort) || json.result.episodes[Math.round(ep_sort - 1)];
return {
    site: "bilibili",
    id: ep.cid,
    url: "https://www.bilibili.com/bangumi/play/ep"+ep.ep_id
}