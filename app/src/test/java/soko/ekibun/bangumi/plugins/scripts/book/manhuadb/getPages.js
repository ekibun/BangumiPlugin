var doc = Jsoup.parse(http.fetch(episode.url).body().string());
var vg_r_data = doc.select(".vg-r-data");
script = doc.select("script").toArray().find(it => it.html().includes("img_data")).html();
eval(script)
var chapterImages = JSON.parse(CryptoJS.enc.Base64.parse(img_data).toString(CryptoJS.enc.Utf8));
var img_prefix = vg_r_data.attr("data-host") + vg_r_data.attr("data-img_pre")
return chapterImages.map(item => ({
    image: {
        url: img_prefix + item.img,
        header: {
            referer: "https://www.manhuadb.com/"
        }
    }
}))