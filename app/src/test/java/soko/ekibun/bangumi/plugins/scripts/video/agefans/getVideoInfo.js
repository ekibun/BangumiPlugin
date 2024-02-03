var ep_sort = episode.sort + Number(line.extra||0)
return {
    site: "agefans",
    id: line.id,
    url: "https://www.agedm.org/play/"+line.id+"/"+ep_sort
}