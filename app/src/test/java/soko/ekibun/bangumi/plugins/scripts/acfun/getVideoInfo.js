var ep_sort = episode.sort + Number(line.extra||0)
var url = "http://www.acfun.cn/album/abm/bangumis/video?albumId="+line.id+"&num="+ep_sort+"&size=1";
var json = JSON.parse(http.get(url).body().string());
var ep = json.data.content[0].videos[0];
return {
    site: "acfun",
    id: ep.danmakuId,
    url: "http://www.acfun.cn/bangumi/aa"+ep.albumId+"_"+ep.groupId+"_"+ep.id
}