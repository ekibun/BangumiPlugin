package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import kotlinx.coroutines.MainScope
import soko.ekibun.bangumi.plugins.service.DownloadService
import soko.ekibun.bangumi.plugins.util.AppUtil
import java.io.File
import java.lang.ref.WeakReference

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

    private val downloadService: DownloadService = DownloadService(host, plugin)

    init {
        File(downloadCachePath).mkdirs()
        appHost!!.remoteAction = { intent, flags, startId ->
            downloadService.onStartCommand(intent, flags, startId)
        }
    }

    companion object {
        val mainScope = MainScope()
        const val ua =
            "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Mobile Safari/537.36"
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

                override fun startActivity(intent: Intent?) {
                    activityRef.get()?.startActivity(intent)
                }
            }
            activityRef.get()?.let { themeContext.applyOverrideConfiguration(it.resources.configuration) }
            return themeContext
        }
    }
}