var doc = JSON.parse(http.post("https://www.manhuagui.com/tools/word.ashx",{},{
    key:key,s:"1"
}).body().string());
return doc.map(it => {
    return {
        site: "manhuagui",
        id: it.u,
        title: it.t
    }
});