package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import kotlinx.coroutines.*
import soko.ekibun.bangumi.plugins.util.AppUtil
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

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


    init {
        File(downloadCachePath).mkdirs()
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

        private val jobCollection = ConcurrentHashMap<String, Job>()
        fun subscribe(
            onError: (t: Throwable) -> Unit = {},
            onComplete: () -> Unit = {},
            key: String? = null,
            block: suspend CoroutineScope.() -> Unit
        ): Job {
            val oldJob = if (key.isNullOrEmpty()) null else jobCollection[key]
            return mainScope.launch {
                try {
                    oldJob?.cancelAndJoin()
                    block.invoke(this)
                } catch (_: CancellationException) {
                } catch (t: Throwable) {
                    Toast.makeText(App.app.host, t.message, Toast.LENGTH_SHORT).show()
                    t.printStackTrace()
                    onError(t)
                }
                if (isActive) onComplete()
            }.also {
                if (!key.isNullOrEmpty()) jobCollection[key] = it
            }
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