package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import soko.ekibun.bangumi.plugins.util.ReflectUtil
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashMap

open class PluginPresenter(activityWeak: WeakReference<Activity>) {
    private val disposables = CompositeDisposable()
    private val keyDisposable = HashMap<String, Disposable>()

    /**
     * 在主线程回调
     * - `onError`中调用`onComplete`保持协同
     * - `onError`默认弹出[Toast]:
     *    ```
     *    Toast.makeText(App.app, it.message, Toast.LENGTH_SHORT).show()
     *    ```
     */
    fun <T> subscribeOnUiThread(
        observable: Observable<T>, onNext: (t: T) -> Unit,
        onError: (t: Throwable) -> Unit = {
            it.printStackTrace()
        },
        onComplete: () -> Unit = {},
        key: String? = null
    ): Disposable {
        if (!key.isNullOrEmpty()) keyDisposable[key]?.dispose()
        return observable.observeOn(AndroidSchedulers.mainThread())
            .subscribe(onNext, {
                if (!it.toString().toLowerCase(Locale.ROOT).contains("canceled")) {
                    Toast.makeText(App.app.host, it.message, Toast.LENGTH_SHORT).show()
                    onError(it)
                }
                onComplete()
            }, onComplete).also {
                if (!key.isNullOrEmpty()) keyDisposable[key] = it
                disposables.add(it)
            }
    }

    fun dispose(key: String) {
        keyDisposable.remove(key)?.dispose()
    }

    private val activityRef = ReflectUtil.proxyObjectWeak(activityWeak, IBaseActivity::class.java)!!
    open fun onDestroy() {
        disposables.dispose()
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