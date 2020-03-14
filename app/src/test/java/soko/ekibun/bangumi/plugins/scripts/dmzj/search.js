var doc = Jsoup.parse(http.get("https://m.dmzj.com/search/" + key + ".html").body().string())
var $ = () => {}
try { eval(doc.select("script").html()) } catch(e) {}
return serchArry.map(it => ({
    site: "dmzj",
    id: it.id,
    title: it.name
}))