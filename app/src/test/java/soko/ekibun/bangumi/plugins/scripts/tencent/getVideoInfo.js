var ep_sort = episode.sort + Number(line.extra||0)
var url = "https://s.video.qq.com/get_playsource?id="+line.id+"&type=4&range="+ep_sort+"-"+(ep_sort + 1)+"&otype=json";
var json = http.fetch(url).body().string();
json = JSON.parse(json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1));
var ep = json.PlaylistItem.videoPlayList.find(it => {
    return Number(it.episode_number) == ep_sort
});
return {
    site: "tencent",
    id: ep.id,
    url: ep.playUrl.split(".html?")[0] + "/"+ ep.id +".html"
}