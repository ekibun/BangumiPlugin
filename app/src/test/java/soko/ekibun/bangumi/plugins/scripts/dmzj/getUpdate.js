var getUpdate = async((page)=>{
    var url = "https://m.dmzj.com/latest/"+ page +".json";
    var json = JSON.parse(http.get(url).body().string());
    return json.map(it => ({
        site: "dmzj",
        id: it.id,
        air: it.last_update_chapter_name
    }));
});
var tasks = [];
for(var i = 0; i < 10; i++){
    tasks.push(getUpdate(i));
}
var ret = [];
for(var i in tasks){
    try{ ret = ret.concat(await(tasks[i])) } catch(e){}
}
return ret;