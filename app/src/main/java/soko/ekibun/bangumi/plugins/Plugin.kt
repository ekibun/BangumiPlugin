package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.Keep
import soko.ekibun.bangumi.plugins.main.MainActivityPlugin
import soko.ekibun.bangumi.plugins.subject.SubjectActivityPlugin
import java.lang.ref.WeakReference

@Keep
object Plugin {
    private val pluginList = mapOf(
        "soko.ekibun.bangumi.ui.subject.SubjectActivity" to SubjectActivityPlugin(),
        "soko.ekibun.bangumi.ui.main.MainActivity" to MainActivityPlugin()
    )

    @Keep
    fun setUpPlugins(activity: Activity, context: Context) {
        Log.v("plugin", activity.javaClass.name)
        App.init(activity.application, context)
        try {
            pluginList[activity.javaClass.name]?.setUpPlugins(WeakReference(activity))
        } catch (e: Exception) {
            Log.e("plugin", Log.getStackTraceString(e))
        }
    }

    interface IApplication {
        var remoteAction: (intent: Intent?, flags: Int, startId: Int) -> Unit
    }
}
