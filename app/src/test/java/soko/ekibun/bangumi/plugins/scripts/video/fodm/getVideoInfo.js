var ep_sort = episode.sort + Number(line.extra||0)
var url = "http://tldm.net"+line.id+"v.html"
var html = http.fetch(url).body().string();
var gid = /\/comment\.asp\?id=(\d+)/g.exec(html)[1];
return {
    site: "fodm",
    id: line.id,
    url: url+"?"+gid+"-0-"+(ep_sort-1)
}