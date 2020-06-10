package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import soko.ekibun.bangumi.plugins.main.MainActivityPlugin
import soko.ekibun.bangumi.plugins.subject.SubjectActivityPlugin
import java.lang.ref.WeakReference

@Keep
class Plugin : BasePlugin() {
    private val pluginList: Map<String, PluginPresenter.Builder> = mapOf(
        "soko.ekibun.bangumi.ui.subject.SubjectActivity" to SubjectActivityPlugin(),
        "soko.ekibun.bangumi.ui.main.MainActivity" to MainActivityPlugin()
    )

    @Keep
    override fun setUpPlugins(activity: Activity, context: Context) {
        super.setUpPlugins(activity, context)
        Log.v("plugin", activity.javaClass.name)
        App.init(activity.application, context)
        try {
            pluginList[activity.javaClass.name]?.setUpPlugins(WeakReference(activity))
        } catch (e: Exception) {
            Log.e("plugin", Log.getStackTraceString(e))
        }
    }
}
