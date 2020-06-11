var getUpdate = async((page)=>{
    var doc = Jsoup.parse(http.fetch("https://m.manhuabei.com/update/?page="+ page).body().string());
    return doc.select("#update_list a.coll").toArray().map(it => {
        var id = /manhua\/([^/]+)/.exec(it.attr("href"))[1];
        return {
            site: "manhuabei",
            id: id,
            air: it.text()
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