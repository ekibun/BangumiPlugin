package soko.ekibun.bangumi.plugins

import android.content.Context
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.model.VideoCacheModel
import soko.ekibun.bangumi.plugins.util.StorageUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class App(val context: Context) {
    val handler = android.os.Handler { true }
    private val databaseProvider by lazy { ExoDatabaseProvider(context) }
    val downloadCache by lazy { SimpleCache(StorageUtil.getDiskCacheDir(context, "video"), NoOpCacheEvictor(), databaseProvider) }
    val jsEngine by lazy { JsEngine(this) }
    val videoCacheModel by lazy { VideoCacheModel(context) }
    val lineProvider by lazy { LineProvider(context) }
    val lineInfoModel by lazy { LineInfoModel(context) }

    companion object {
        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool()

        var app: App? = null
        fun from(context: Context): App {
            app = app?:App(context)
            return app!!
        }
    }
}