package soko.ekibun.bangumi.plugins

import android.util.Log
import android.webkit.WebResourceRequest
import androidx.annotation.Keep
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import soko.ekibun.bangumi.plugins.ui.view.BackgroundWebView
import soko.ekibun.bangumi.plugins.util.HttpUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil
import java.util.*
import java.util.concurrent.Callable
import kotlin.collections.HashMap

class JsEngine {
    private val webviewList = WeakHashMap<String, BackgroundWebView>()
    @Keep
    fun webviewload(
        key: String,
        url: String,
        header: Map<String, String>,
        script: String,
        onInterceptRequest: (WebResourceRequest) -> HttpUtil.HttpRequest?
    ): HttpUtil.HttpRequest? {
        var ret: HttpUtil.HttpRequest? = null
        var finished = false
        var pageFinish = false
        var lastTime = System.currentTimeMillis()
        App.app.handler.post {
            val webview = webviewList.getOrPut(key) { BackgroundWebView(App.app.host) }
            webview.settings.userAgentString = header["User-Agent"] ?: webview.settings.userAgentString
            val map = HashMap<String, String>()
            map["referer"] = url
            map.putAll(header)
            webview.onInterceptRequest = {
                onInterceptRequest(it)?.let {
                    ret = it
                    finished = true
                    webview.onInterceptRequest = {}
                    webview.onPageFinished = {}
                }
                lastTime = System.currentTimeMillis()
            }
            webview.onPageFinished = {
                webview.evaluateJavascript(script) {
                    JsonUtil.toEntity<HttpUtil.HttpRequest>(it)?.let {
                        ret = it
                        finished = true
                        webview.onInterceptRequest = {}
                        webview.onPageFinished = {}
                    }
                }
                lastTime = System.currentTimeMillis()
                pageFinish = true
            }
            webview.loadUrl(url, map)
        }
        while (!finished) {
            Thread.sleep(1000)
            if (pageFinish && System.currentTimeMillis() - lastTime > 30 * 1000) {//30sec Timeout
                return ret
            }
        }
        return ret
    }

    @Keep
    fun print(data: String) {
        if (App.inited) Log.v("jsEngine", data) else println(data)
    }

    @Keep
    fun storePreference(key: String, data: String) {
        if (App.inited) App.app.sp.edit().putString(key, data).apply()
    }

    @Keep
    fun readPreference(key: String): String {
        return if (App.inited) App.app.sp.getString(key, null) ?: "" else ""
    }

    fun runScript(script: String, header: String?, key: String): String {
        val globalMethods = """
            |var _http = Packages.${HttpUtil.javaClass.name}.INSTANCE;
            |var AsyncTask = Packages.${JsAsyncTask::class.java.name};
            |var _cachedThreadPool = Packages.${App::class.java.name}.Companion.getCachedThreadPool();
            |var Jsoup = Packages.org.jsoup.Jsoup;
            |var http = {
            |   get(url, header) {
            |       return _http.getCall(url, header||{}, null).execute();
            |   },
            |   post(url, header, data, mediaType){
            |       if(mediaType) return _http.getCall(url, header||{}, _http.createBody(mediaType, data)).execute();
            |       else return _http.getCall(url, header||{}, _http.createBody(data)).execute();
            |   },
            |   inflate(bytes, encoding){
            |       if(encoding == "deflate"){
            |           return "" + new java.lang.String(_http.inflate(bytes, true));
            |       }else if(encoding == "gzip"){
            |           return "" + new java.lang.String(_http.inflate(bytes, false));
            |       }else return "" + new java.lang.String(bytes, encoding);
            |   },
            |   html2text(html){
            |       return Jsoup.parse(html).wholeText()
            |   }
            |}
            |var webview = {
            |   toRequest(request){
            |       return _http.makeRequest(request);
            |   },
            |   load(url, header, script, onInterceptRequest){
            |       return _jsEngine.webviewload(${JsonUtil.toJson(key)}, url||"", header||{}, script||"", onInterceptRequest||function(request){
            |           if(request.getRequestHeaders().get("Range") != null) return webview.toRequest(request);
            |           else return null;
            |       });
            |   }
            |}
            |function async(fun){
            |   return (param)=>{
            |       var task = new AsyncTask(function(params){ try { return JSON.stringify(fun(params)) } catch(e){ return new Error(e) } }, param)
            |       return _cachedThreadPool.submit(task)
            |   }
            |}
            |function await(task){
            |   var data = task.get()
            |   if(data instanceof Error) throw data.message
            |   return JSON.parse(data)
            |}
            |function print(obj){
            |   _jsEngine.print(${JsonUtil.toJson(key)} + ": " + obj);
            |}
            |var App = {
            |   dump(key, data) {
            |       _jsEngine.storePreference(key, JSON.stringify(data))
            |   },
            |   load(key) {
            |       data = _jsEngine.readPreference(key)
            |       return data && JSON.parse(data)
            |   }
            |}
            |
            |function handleCircular() {
            |   var cache = []
            |   var keyCache = []
            |   return (key, value) => {
            |       if(value instanceof Packages.java.lang.Object) {
            |           return JSON.parse(Packages.${JsonUtil.javaClass.name}.INSTANCE.toJson(value));
            |       }
            |       if (typeof value === 'object' && value !== null) {
            |           var index = cache.indexOf(value);
            |           if (index !== -1) return '[Circular ' + keyCache[index] + ']'
            |           cache.push(value)
            |           keyCache.push(key || 'root')
            |       }
            |       return value
            |   }
            |}
            |
            |var tmp = JSON.stringify;
            |JSON.stringify = function(value, replacer, space) {  
            |   replacer = replacer || handleCircular();
            |   return tmp(value, replacer, space);
            |}
        """.trimMargin()

        return try {
            val rhino = Context.enter()
            rhino.wrapFactory.isJavaPrimitiveWrap = false
            rhino.optimizationLevel = -1
            val scope = rhino.initStandardObjects()
            ScriptableObject.putProperty(scope, "_jsEngine", Context.javaToJS(this, scope))
            rhino.evaluateString(scope, globalMethods, "global", 1, null)
            if (!header.isNullOrEmpty()) rhino.evaluateString(scope, header, "header", 1, null)
            rhino.evaluateString(
                scope, """(function(){var _ret = (function(){$script
                |   }());
                |   return JSON.stringify(_ret);
                |}())""".trimMargin(), "script", 0, null
            ).toString()
        } finally {
            webviewList.remove(key)?.finish()
            Context.exit()
        }
    }

    class JsAsyncTask(val js: (Any?) -> Any?, private val params: Any?) : Callable<Any?> {
        override fun call(): Any? {
            return js(params)
        }
    }

    companion object {
        fun <T> makeScript(
            jsEngine: JsEngine,
            script: String,
            header: String?,
            key: String,
            converter: (String) -> T
        ): Observable<T> {
            return Observable.just(0).observeOn(Schedulers.io()).map {
                converter(jsEngine.runScript(script, header, key))
            }
        }
    }
}