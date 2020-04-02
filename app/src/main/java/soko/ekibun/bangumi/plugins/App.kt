package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import soko.ekibun.bangumi.plugins.model.EpisodeCacheModel
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.service.DownloadService
import soko.ekibun.bangumi.plugins.util.AppUtil
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class App(host: Context, plugin: Context) : BaseApp(host, plugin) {
    val handler = android.os.Handler { true }
    private val databaseProvider by lazy { ExoDatabaseProvider(host) }
    private val downloadCachePath = AppUtil.getDiskFileDir(host, "download").absolutePath

    val downloadCache by lazy {
        SimpleCache(
            File(downloadCachePath + File.separator + "video"),
            NoOpCacheEvictor(),
            databaseProvider
        )
    }
    val jsEngine by lazy { JsEngine() }
    val episodeCacheModel by lazy { EpisodeCacheModel(plugin) }
    val lineProvider by lazy { LineProvider(plugin) }
    val lineInfoModel by lazy { LineInfoModel(plugin) }

    private val downloadService: DownloadService = DownloadService(host, plugin)

    init {
        File(downloadCachePath).mkdirs()
        appHost!!.remoteAction = { intent, flags, startId ->
            downloadService.onStartCommand(intent, flags, startId)
        }
    }

    companion object {
        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool()

        val inited get() = ::app.isInitialized

        lateinit var app: App
        fun init(host: Context, plugin: Context) {
            if (!inited) app = App(host, plugin)
        }

        fun createThemeContext(activityRef: WeakReference<Activity>): Context {
            val themeContext = object : ContextThemeWrapper(app.plugin, R.style.AppTheme) {
                override fun getApplicationContext(): Context {
                    return this
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