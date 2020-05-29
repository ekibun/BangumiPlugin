var arr = [];
function getEpisode(page){
    var url = PICA_HOST + "/comics/"+line.id+"/eps?page="+page;
    var json = JSON.parse(http.fetch(url, {
        headers: headerBuild("get", url, {
            "authorization": token
        })
    }).body().string());
    var list = json.data.eps.docs.map(it =>{
        return {
            site: "pica",
            id: it._id,
            sort: it.order,
            title: it.title,
            url: PICA_HOST+"/comics/"+line.id+"/order/"+it.order+"/pages"
        }
    });
    arr = arr.concat(list);
    if(page < json.data.eps.pages) getEpisode(page+1);
}
getEpisode(1);
return arr.reverse();