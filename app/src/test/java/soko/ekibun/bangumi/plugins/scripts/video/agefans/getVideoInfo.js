var ep_sort = episode.sort + Number(line.extra||0)
return {
    site: "agefans",
    id: line.id,
    url: "https://www.agefans.tv/play/"+line.id+"_"+ep_sort
}