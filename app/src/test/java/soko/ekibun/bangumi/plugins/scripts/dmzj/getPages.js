var doc = Jsoup.parse(http.fetch(episode.url).body().string())
var $ = () => ({ ready: (a) => a() })
var document = {}
var mReader = { initData: (a) => { throw a } }
try { eval(doc.select("script").toArray().map((v) => v.html()).find((v) => v.includes("mReader.initData"))) }
catch(e){ data = e.page_url }
return data.map(it => ({
    image: {
        url: it,
        header: {
            referer: "https://m.dmzj.com/"
        }
    }
}))