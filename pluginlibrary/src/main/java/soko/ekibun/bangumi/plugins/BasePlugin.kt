package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.Keep
import java.lang.ref.WeakReference

@Keep
abstract class BasePlugin : Service() {
    abstract val pluginList: Map<String, ActivityPlugin>

    @Keep
    open fun setUpPlugins(activity: Activity, context: Context) {
        try {
            pluginList[activity.javaClass.name]?.setUpPlugins(WeakReference(activity))
        } catch (e: Exception) {
            Log.e("plugin", Log.getStackTraceString(e))
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
