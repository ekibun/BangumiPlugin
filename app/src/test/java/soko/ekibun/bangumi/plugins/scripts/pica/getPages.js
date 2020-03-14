var arr = []
function getImage(page){
    var url = episode.url+"?page="+page;
    var json = JSON.parse(http.get(url, headerBuild("get", url, {
        "authorization": token
    })).body().string())
    var list = json.data.pages.docs.map(it =>{
        return {
            image: {
                url: "https://s3.picacomic.com/static/"+it.media.path
            }
        }
    })
    arr = arr.concat(list);
    if(page < json.data.pages.pages) getImage(page+1);
}
getImage(1);
return arr;