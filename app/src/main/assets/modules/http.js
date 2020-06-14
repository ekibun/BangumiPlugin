var okhttp3 = Packages.okhttp3;
var ua = __pkg__.App.ua

var httpLoggingInterceptor = new okhttp3.logging.HttpLoggingInterceptor();
httpLoggingInterceptor.level(okhttp3.logging.HttpLoggingInterceptor.Level.BASIC);

var httpClientBuilder = (new okhttp3.OkHttpClient.Builder())
    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
    .addNetworkInterceptor(httpLoggingInterceptor)
    .addInterceptor(okhttp3.brotli.BrotliInterceptor.INSTANCE);

var httpClient = [httpClientBuilder.build(), httpClientBuilder.followRedirects(false).build()];

var handler = __TEST__ || new Packages.android.os.Handler(android.os.Looper.getMainLooper());
/**
 * webview
 */
function webviewload(key, url, header, script, onInterceptRequest) {
    var ret = undefined;
    var finished = false;
    var pageFinish = false;
    if (__TEST__) return {};
    handler.post(() => {
        var webview = __webview_list__.get(key);
        if(!webview) {
            webview = new __pkg__.ui.view.BackgroundWebView(__pkg__.App.app.getHost());
            __webview_list__.put(key, webview);
        }
        webview.getSettings().setUserAgentString((header && header["User-Agent"]) || ua);
        header.referer = header.referer || url;
        webview.setOnInterceptRequest((request) => {
            var it = onInterceptRequest(request);
            if(it) {
                ret = it;
                finished = true;
                webview.setOnInterceptRequest(function () {});
                webview.setOnPageFinished(function () {});
            }
            lastTime = java.lang.System.currentTimeMillis();
        })
        webview.setOnPageFinished(() => {
            if(script) webview.evaluateJavascript(script, (it) => {
                it = it && JSON.parse(it);
                if(it) {
                    ret = it;
                    finished = true;
                    webview.setOnInterceptRequest(function () {});
                    webview.setOnPageFinished(function () {});
                }
            })
            lastTime = java.lang.System.currentTimeMillis();
            pageFinish = true;
        })
        webview.loadUrl(url, header);
    })
    while(!finished) {
        java.lang.Thread.sleep(1000);
        if (pageFinish && java.lang.System.currentTimeMillis() - lastTime > 10000) {//10sec Timeout
            return ret;
        }
    }
    return ret;
}

/**
 * gzip解压缩
 */
function inflate(data, nowrap) {
    var output = data;
    var inflater = new java.util.zip.Inflater(nowrap);
    inflater.reset();
    inflater.setInput(data);
    var o = new java.io.ByteArrayOutputStream(data.length);
    try {
        var buf = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, 1024);
        while (!inflater.finished()) {
            var i = inflater.inflate(buf);
            o.write(buf, 0, i);
        }
        output = o.toByteArray();
    } catch (e) {
        e.printStackTrace();
    } finally {
        try {
            o.close();
        } catch (e) {
            e.printStackTrace();
        }
    }
    inflater.end();
    return output;
}
/**
 * 把webview请求转成HttpRequest
 */
function makeRequest(request) {
    var cookieManager = android.webkit.CookieManager.getInstance();
    cookieManager.flush();
    var headers = request.getRequestHeaders();
    headers = JSON.parse(JSON.stringify(headers));
    headers["cookie"] = cookieManager.getCookie(request.getUrl().getHost()) || "";
    return {
        url: request.getUrl().toString(),
        header: headers,
    };
}

module.exports = {
    fetch(url, options) {
        options = options || {};
        var header = options.headers || {};
        header["User-Agent"] = header["User-Agent"] || ua;
        var request = (new okhttp3.Request.Builder()).url(url).headers(okhttp3.Headers.of(header));
        if(options.body) {
           if(options.contentType) {
               var body = okhttp3.RequestBody.create(options.body, okhttp3.MediaType.parse(options.contentType));
               request.post(body);
           } else {
               var body = new okhttp3.FormBody.Builder()
               for (k in options.body) body.add(k, options.body[k])
               request.post(body.build());
           }
        }
        return httpClient[options.followRedirect === false ? 1 : 0].newCall(request.build()).execute();
    },
    inflate(bytes, encoding){
        if(encoding == "deflate"){
            return "" + new java.lang.String(inflate(bytes, true));
        }else if(encoding == "gzip"){
            return "" + new java.lang.String(inflate(bytes, false));
        }else return "" + new java.lang.String(bytes, encoding);
    },
    __webview__(url, header, script, onInterceptRequest){
        return webviewload(this.__env_key__, url||"", header||{}, script, onInterceptRequest || function(request){
            if(request.getRequestHeaders().get("Range") != null)
                return makeRequest(request);
            else return null;
        });
    },
    makeRequest: makeRequest
}