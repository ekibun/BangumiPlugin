var ep_sort = episode.sort + Number(line.extra||0)
return {
    site: "hehua",
    id: line.id,
    url: "https://www.hehua.net/"+line.id+"-"+ep_sort+".html"
}