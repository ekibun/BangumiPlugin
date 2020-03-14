var url = "https://www.ningmoe.com/api/search";
var json = JSON.parse(http.post(url, {}, JSON.stringify({
    token: null,
    keyword: key,
    type: "anime",
    bangumi_type: "",
    page: 1,
    limit: 10
}), "application/json;charset=utf-8").body().string());
return json.data.map(it => {
    return {
        site: "ningmoe",
        id: it.bangumi_id,
        title: it.classification.cn_name || it.classification.en_name
    };
});