package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.model.VideoCacheModel
import soko.ekibun.bangumi.plugins.service.DownloadService
import soko.ekibun.bangumi.plugins.util.ReflectUtil
import soko.ekibun.bangumi.plugins.util.StorageUtil
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class App(val host: Context, val plugin: Context) {
    val handler = android.os.Handler { true }
    private val databaseProvider by lazy { ExoDatabaseProvider(host) }
    val downloadCache by lazy {
        SimpleCache(
            StorageUtil.getDiskCacheDir(host, "video"),
            NoOpCacheEvictor(),
            databaseProvider
        )
    }
    val jsEngine by lazy { JsEngine(this) }
    val videoCacheModel by lazy { VideoCacheModel(plugin) }
    val lineProvider by lazy { LineProvider(plugin) }
    val lineInfoModel by lazy { LineInfoModel(plugin) }

    private val downloadService: DownloadService = DownloadService(host, plugin)

    init {
        ReflectUtil.proxyObject(host, Plugin.IApplication::class.java)!!.remoteAction =
            { intent, flags, startId -> downloadService.onStartCommand(intent, flags, startId) }
    }

    companion object {
        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool()

        lateinit var app: App
        fun init(host: Context, plugin: Context) {
            if (!::app.isInitialized) app = App(host, plugin)
        }

        fun createThemeContext(activityRef: WeakReference<Activity>): Context {
            val themeContext = object : ContextThemeWrapper(app.plugin, R.style.AppTheme) {
                override fun getApplicationContext(): Context {
                    return app.plugin
                }

                override fun getSystemService(name: String): Any? {
                    return when (name) {
                        Context.WINDOW_SERVICE -> activityRef.get()?.getSystemService(name)
                        else -> super.getSystemService(name)
                    }
                }
            }
            activityRef.get()?.let { themeContext.applyOverrideConfiguration(it.resources.configuration) }
            return themeContext
        }
    }
}