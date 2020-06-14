package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.widget.Toast
import kotlinx.coroutines.*
import soko.ekibun.bangumi.plugins.util.ReflectUtil
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

open class PluginPresenter(activityWeak: WeakReference<Activity>) : CoroutineScope by MainScope() {
    private val jobCollection = ConcurrentHashMap<String, Job>()
    fun cancel(check: (String) -> Boolean) {
        jobCollection.keys.forEach {
            if (check(it)) jobCollection.remove(it)
        }
    }

    fun subscribe(
        onError: (t: Throwable) -> Unit = {},
        onComplete: () -> Unit = {},
        key: String? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val oldJob = if (key.isNullOrEmpty()) null else jobCollection[key]
        return launch {
            try {
                oldJob?.cancelAndJoin()
                block.invoke(this)
            } catch (_: CancellationException) {
            } catch (t: Throwable) {
                Toast.makeText(App.app.host, t.message, Toast.LENGTH_SHORT).show()
                onError(t)
            }
            if (isActive) onComplete()
        }.also {
            if (!key.isNullOrEmpty()) jobCollection[key] = it
        }
    }

    private val activityRef = ReflectUtil.proxyObjectWeak(activityWeak, IBaseActivity::class.java)!!
    open fun onDestroy() {
        cancel()
    }

    init {
        activityRef.onDestroyListener = ::onDestroy
    }

    interface IBaseActivity {
        var onDestroyListener: () -> Unit
    }

    interface Builder {
        fun setUpPlugins(activityRef: WeakReference<Activity>)
    }
}