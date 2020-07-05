var lineData = JSON.parse(line.id);
var url = "https://www.pixiv.net/ajax/user/" + lineData.user + "/illustmanga/tag?offset=0&limit=1000&tag=" + lineData.tag
return JSON.parse(http.fetch(url).body().string()).body.works.map((it, i, arr) => ({
    site: "pixiv",
    id: it.id,
    sort: arr.length - i,
    title: it.title,
    url: "https://www.pixiv.net/artworks/" + it.id
})).reverse()