function randomKey(len){
    var s = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
    var r = ""
    for(var i=0; i<len;i++) r += s.charAt(Math.random() * s.length | 0);
    return r;
}

function getParams(query) {
    query = JSON.stringify(query)
    var nonce = CryptoJS.enc.Utf8.parse('0CoJUm6Qyw8W8jud');
    var iv = CryptoJS.enc.Utf8.parse('0102030405060708');
    var modulus = '00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7';
    var pubKey = '010001';
    var genKey = randomKey(16);
    var aes = CryptoJS.AES.encrypt(CryptoJS.enc.Utf8.parse(query), nonce, {
        iv: iv,
        mode: CryptoJS.mode.CBC
    }).toString()
    var params = CryptoJS.AES.encrypt(aes, CryptoJS.enc.Utf8.parse(genKey), {
        iv: iv,
        mode: CryptoJS.mode.CBC
    }).toString()
    var integer = new java.math.BigInteger(genKey.split('').reverse().map(c => {
        return c.charCodeAt(0).toString(16)
    }).join(''), 16)
    var pubkeyInt = new java.math.BigInteger(pubKey, 16);
    var modulusInt = new java.math.BigInteger(modulus, 16);
    var encSecKey = integer.modPow(pubkeyInt, modulusInt).toString(16)
    encSecKey = (new Array(256).join('0') + encSecKey).slice(-256)
    return {
        params: params,
        encSecKey: encSecKey
    }
}

function fetch(url, body) {
    return http.fetch(url, {
        headers: {
            referer: "http://music.163.com",
            cookie: "os=uwp; osver=10.0.10586.318; appver=1.2.1;"
        },
        body: getParams(body)
    });
}