var url = PICA_HOST + "/comics/search?q="+java.net.URLEncoder.encode(key)+"&page=1";
var json = JSON.parse(http.fetch(url, {
    headers: headerBuild("get", url, {
        "authorization": token
    })
})).body().string())
return json.data.comics.docs.map(it =>{
    return {
        site: "pica",
        id: it._id,
        title: it.title
    }
})