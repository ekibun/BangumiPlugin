package soko.ekibun.bangumi.plugins

import android.content.Context
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import java.util.concurrent.Executors
import java.util.logging.Handler

class App(val context: Context) {
    val handler = android.os.Handler { true }
    val jsEngine by lazy { JsEngine(this) }
    val lineProvider by lazy { LineProvider(context) }
    val lineInfoModel by lazy { LineInfoModel(context) }

    companion object {
        val cachedThreadPool = Executors.newCachedThreadPool()

        var app: App? = null
        fun from(context: Context): App {
            app = app?:App(context)
            return app!!
        }
    }
}