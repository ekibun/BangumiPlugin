var url = "https://api.bilibili.com/x/web-interface/search/type?search_type=media_bangumi&keyword=";
var json = JSON.parse(http.get(url+key).body().string());
return json.data.result.map(it => {
    return {
        site: "bilibili",
        id: it.media_id,
        title: Jsoup.parse(it.title).text()
    };
});