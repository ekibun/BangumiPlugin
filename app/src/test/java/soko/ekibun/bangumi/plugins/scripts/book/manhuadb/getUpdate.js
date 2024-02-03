var getUpdate = async((page)=>{
    var doc = Jsoup.parse(http.fetch("https://www.manhuadb.com/update_" + page + ".html").body().string());
    return doc.select(".comicbook-index .update_title > a").toArray().map(it => {
        var id = /manhua\/([^/]+)/.exec(it.attr("href"))[1];
        return {
            site: "manhuadb",
            id: id,
            air: it.text()
        }
    });
});
var tasks = [];
for(var i = 0; i < 10; i++){
    tasks.push(getUpdate(i+1));
}
var ret = [];
for(var i in tasks){
    try{ ret = ret.concat(await(tasks[i])) } catch(e){ }
}
return ret;