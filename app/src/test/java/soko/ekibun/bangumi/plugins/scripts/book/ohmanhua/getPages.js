var doc = Jsoup.parse(http.fetch(episode.url).body().string())
function __cdecrypt(key, word) {
    var key = CryptoJS.enc.Utf8.parse(key);
    var decrypt = CryptoJS.AES.decrypt(word, key, {
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.Pkcs7
    });
    return CryptoJS.enc.Utf8.stringify(decrypt).toString();
}
var script = doc.select("script").toArray().find(it => it.html().includes("C_DATA")).html();
eval(script);
C_DATA = __cdecrypt("JRUIFMVJDIWE569j", CryptoJS.enc.Base64.parse(C_DATA).toString(CryptoJS.enc.Utf8));
eval(C_DATA);
var _0x963cx1a = CryptoJS.enc.Base64.parse(image_info.urls__direct).toString(CryptoJS.enc.Utf8);
var __images_yy = _0x963cx1a.split('|SEPARATER|');
return (__images_yy && __images_yy.length > 1 ? __images_yy : new Array(mh_info.totalimg).fill(0).map((it, index) =>
        "http://img.mljzmm.com/comic/" +  mh_info.imgpath + ('000'+(index + mh_info.startimg)).slice(-4) + ".jpg")
).map(it => ({
    image: {
        url: it
    }
}));