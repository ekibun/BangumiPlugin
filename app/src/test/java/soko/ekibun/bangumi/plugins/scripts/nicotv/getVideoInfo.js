var ep_sort = episode.sort + Number(line.extra||0)
return {
    site: "nicotv",
    id: line.id,
    url: "http://www.nicotv.me/video/play/"+line.id+"-"+ep_sort+".html"
}