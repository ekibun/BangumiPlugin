var ep_sort = episode.sort + Number(line.extra||0)
var url = "http://apis.web.pptv.com/show/videoList?format=jsonp&pid="+line.id;
var json = JSON.parse(http.fetch(url).body().string());
var ep = json.data.list[(ep_sort - 1) | 0];
return {
    site: "pptv",
    id: ep.id,
    url: "http:" + ep.url
}