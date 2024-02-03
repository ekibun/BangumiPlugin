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
try {
    C_DATA = __cdecrypt("fw12558899ertyui", CryptoJS.enc.Base64.parse(C_DATA).toString(CryptoJS.enc.Utf8));
} catch (error) {
    C_DATA = __cdecrypt("JRUIFMVJDIWE569j", CryptoJS.enc.Base64.parse(C_DATA).toString(CryptoJS.enc.Utf8));
}
eval(C_DATA);
var _0x1190x84
if (typeof mh_info.enc_code2 == "undefined") {
    _0x1190x84 = mh_info.imgpath
} else {
    var _0x1190x85 = "fw125gjdi9ertyui";
    var _0x1190x86;
    try {
        _0x1190x86 = __cdecrypt(_0x1190x85, CryptoJS.enc.Base64.parse(mh_info.enc_code2).toString(CryptoJS.enc.Utf8))
    } catch (error) {
        _0x1190x86 = __cdecrypt("", CryptoJS.enc.Base64.parse(mh_info.enc_code2).toString(CryptoJS.enc.Utf8))
    }
    ;_0x1190x84 = _0x1190x86
}
var totalImageCount
if (typeof mh_info.enc_code1 == "undefined") {
    totalImageCount = mh_info.totalimg
} else {
    var DECRIPT_DATA;
    try {
        DECRIPT_DATA = __cdecrypt("fw12558899ertyui", CryptoJS.enc.Base64.parse(mh_info.enc_code1).toString(CryptoJS.enc.Utf8));
    } catch (error) {
        DECRIPT_DATA = __cdecrypt("JRUIFMVJDIWE569j", CryptoJS.enc.Base64.parse(mh_info.enc_code1).toString(CryptoJS.enc.Utf8));
    }
    totalImageCount = eval(DECRIPT_DATA);
}
return (new Array(totalImageCount).fill(0).map((it, index) =>
        "https://img.cocomanhua.com/comic/" +  _0x1190x84 + ('000'+(index + mh_info.startimg)).slice(-4) + ".jpg")
).map(it => ({
    image: {
        url: it
    }
}));