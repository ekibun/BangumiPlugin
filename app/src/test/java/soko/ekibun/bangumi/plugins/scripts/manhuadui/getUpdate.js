var getUpdate = async((page)=>{
    var url = "https://m.manhuadui.com/update/?page="+ page;
    var doc = Jsoup.parse(http.get(url).body().string());
    return doc.select("#update_list a.coll").toArray().map(it => {
        var id = /manhua\/([^/]+)/.exec(it.attr("href"))[1];
        return {
            site: "manhuadui",
            id: id,
            air: "更新到" + String(it.text())
        }
    });
});
var tasks = [];
for(var i = 0; i < 10; i++){
    tasks.push(getUpdate(i));
}
var ret = [];
for(var i in tasks){
    try{ ret = ret.concat(await(tasks[i])) } catch(e){ }
}
return ret;