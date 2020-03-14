function decode(data) {
    var Base64 = Packages.android.util.Base64
    var crypto = Packages.javax.crypto
    var key = new java.lang.String("123456781234567G").getBytes()
    var ivs = new java.lang.String("ABCDEF1G34123412").getBytes()
    var cipher = crypto.Cipher.getInstance("AES/CBC/PKCS7Padding")
    var secretKeySpec = crypto.spec.SecretKeySpec(key, "AES")
    var paramSpec = crypto.spec.IvParameterSpec(ivs)
    cipher.init(crypto.Cipher.DECRYPT_MODE, secretKeySpec, paramSpec)
    return new java.lang.String(cipher.doFinal(Base64.decode(data, Base64.DEFAULT)))
}
var doc = Jsoup.parse(http.get(episode.url).body().string());
var script = doc.select("script").toArray().find(it => it.html().includes("chapterImages")).html()
eval(script)
return JSON.parse(decode(chapterImages)).map(item => ({
    image: {
        url: item.startsWith("http")? item : "https://mhcdn.manhuazj.com/" + chapterPath + item,
        header: {
            referer: item.includes("dmzj")? "https://m.dmzj.com/" : "https://m.manhuadui.com/"
        }
    }
}))