var url="http://search.aixifan.com/search?q=";
var json = JSON.parse(http.get(url+key).body().string());
return json.data.page.ai.map(it => {
    return {
        site: "acfun",
        id: it.contentId,
        title: it.title
    };
});