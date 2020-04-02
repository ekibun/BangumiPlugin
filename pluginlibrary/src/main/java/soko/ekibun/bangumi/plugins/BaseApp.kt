package soko.ekibun.bangumi.plugins

import android.content.Context
import android.content.Intent
import soko.ekibun.bangumi.plugins.util.ReflectUtil

abstract class BaseApp(val host: Context, val plugin: Context) {
    val appHost = ReflectUtil.proxyObject(host, IApplication::class.java)

    interface IApplication {
        var remoteAction: (intent: Intent?, flags: Int, startId: Int) -> Unit
    }
}