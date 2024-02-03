var jsonstr = fetch("http://music.163.com/weapi/cloudsearch/get/web?csrf_token=", {
                  s: key,
                  limit: 10,
                  offset: 0,
                  total: true,
                  csrf_token: "",
                  type: 10
              }).body().string()
throw jsonstr
var json = JSON.parse(fetch("http://music.163.com/weapi/cloudsearch/get/web?csrf_token=", {
    s: key,
    limit: 10,
    offset: 0,
    total: true,
    csrf_token: "",
    type: 10
}).body().string())

return json.result.albums.map(it => ({
    site: "netease",
    id: it.id,
    title: it.name
}))