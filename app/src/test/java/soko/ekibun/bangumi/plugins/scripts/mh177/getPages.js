var doc = Jsoup.parse(http.get(episode.url).body().string());
eval(doc.selectFirst("script").html())
return msg.split("|").map(it => ({
    image: {
        url:"https://picsh.77dm.top/h"+img_s+"/"+it+".webp"
    }
}))