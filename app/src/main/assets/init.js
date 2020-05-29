var Jsoup = Packages.org.jsoup.Jsoup;

var __pkg__ = Packages.soko.ekibun.bangumi.plugins;
var __TEST__ = !__pkg__.App.Companion.getInited();

require("json-wrapper");
var http = require("http");
var App = require("app");

/**
 * 输出wrapper
 */
function print(data) {
    data = String(this.__env_key__ + ": " + data);
    if (__TEST__)
        java.lang.System.out.println(data);
    else
        android.util.Log.v('js', data);
}

/**
 * async 多线程
 */
var __cachedThreadPool = java.util.concurrent.Executors.newCachedThreadPool()
function async(fun){
   return (param) => {
       var task = new java.util.concurrent.Callable({
           call: () => {
               try { return JSON.stringify(fun(param)) } catch(e){ return new Error(e) }
           }
       })
       return __cachedThreadPool.submit(task)
   }
}
function await(task){
   var data = task.get()
   if(data instanceof Error) throw data.message
   return JSON.parse(data)
}