var doc = Jsoup.parse(http.fetch("https://so.177mh.net/k.php?k=" + key).body().string());
return doc.select("div.ar_list_co dl h1>a").toArray().map(it => {
    return {
        site: "mh177",
        id: /colist_(\d+)/.exec(it.attr("href"))[1],
        title: it.text()
    }
});