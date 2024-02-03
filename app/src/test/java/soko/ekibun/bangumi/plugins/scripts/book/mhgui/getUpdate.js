var getUpdate = async((page)=>{
    var doc = Jsoup.parse(http.fetch("https://m.mhgui.com/update/?page="+page+"&ajax=1&order=1").body().string());
    return doc.select("li > a").toArray().map(it => {
        var id = /manhua\/([^/]+)/.exec(it.attr("href"))[1];
        return {
            site: "mhgui",
            id: id,
            air: it.selectFirst("h3").text()
        }
    });
});
var tasks = [];
for(var i = 1; i < 6; i++){
    tasks.push(getUpdate(i));
}
var ret = [];
for(var i in tasks){
    try{ ret = ret.concat(await(tasks[i])) } catch(e){ }
}
return ret;