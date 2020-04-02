package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import soko.ekibun.bangumi.plugins.main.MainActivityPlugin
import soko.ekibun.bangumi.plugins.subject.SubjectActivityPlugin

@Keep
class Plugin : BasePlugin() {
    override val pluginList: Map<String, ActivityPlugin> = mapOf(
        "soko.ekibun.bangumi.ui.subject.SubjectActivity" to SubjectActivityPlugin(),
        "soko.ekibun.bangumi.ui.main.MainActivity" to MainActivityPlugin()
    )

    @Keep
    override fun setUpPlugins(activity: Activity, context: Context) {
        Log.v("plugin", activity.javaClass.name)
        App.init(activity.application, context)
        super.setUpPlugins(activity, context)
    }
}
