const PICA_BASE_HOST = "picaapi.picacomic.com";
const PICA_HOST = "https://" + PICA_BASE_HOST;

var url = PICA_HOST + "/auth/sign-in"
var token = App.load("pica_token")
if(!token) {
    token = JSON.parse(http.fetch(url, {
        headers: headerBuild("post", url, {}),
        body: JSON.stringify({
            email: "",
            password: "",
        }),
        contentType: "application/json; charset=UTF-8"
    }).body().string()).data.token;
    App.dump("pica_token", token)
}


function HMACSHA256(data, key) {
  var crypto = Packages.javax.crypto;
  var signingKey = crypto.spec.SecretKeySpec(key, "HmacSHA256");
  var mac = crypto.Mac.getInstance("HmacSHA256");
  mac.init(signingKey);
  return mac.doFinal(data).map(byte => ('0'+(byte & 0xFF).toString(16)).slice(-2)).join("");
}

function headerBuild(method, url, header){
  var time = Math.floor(new Date().getTime() / 1000);
  var uuid = java.util.UUID.randomUUID().toString();
  var nonce = uuid.replace(/-/g, "");
  var api_key = "C69BAF41DA5ABD1FFEDC6D2FEA56B";
  var secret_key = new java.lang.String("~d}\$Q7\$eIni=V)9\\RK/P.RM4;9[7|@/CA}b~OW!3?EV`:<>M7pddUBL5n|0/*Cn").getBytes();
  url = url.replace(PICA_HOST+"/", "");
  var signature = HMACSHA256(new java.lang.String((url + time + nonce + method + api_key).toLowerCase()).getBytes(), secret_key);

  return Object.assign(header, {
    "Content-Type": "application/json; charset=UTF-8",
    "Host": PICA_BASE_HOST,
    "User-Agent": "okhttp/3.8.1",
    "accept": "application/vnd.picacomic.com.v1+json",
    "api-key": api_key,
    "app-build-version": "44",
    "app-version": "2.2.1.2.3.3",
    "app-channel": "1",
    "app-platform": "android",
    "app-uuid": uuid,
    "nonce": nonce,
    "sources": "MHViewer0.0.3",
    "time": time.toString(),
    "signature": signature,
    "image-quality": "original"
  });
}