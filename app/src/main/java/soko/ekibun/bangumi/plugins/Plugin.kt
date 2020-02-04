package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.annotation.Keep
import soko.ekibun.bangumi.plugins.subject.SubjectActivityPlugin
import java.lang.Exception

@Keep
object Plugin {
    private val pluginList = mapOf<String, ActivityPlugin>(
        "soko.ekibun.bangumi.ui.subject.SubjectActivity" to SubjectActivityPlugin()
    )

    @Keep
    fun setUpPlugins(activity: Activity, context: Context) {
        Log.v("plugin", activity.javaClass.name)
        try {
            context.setTheme(R.style.AppTheme)
            pluginList[activity.javaClass.name]?.setUpPlugins(activity, object: ContextWrapper(context){
                override fun getApplicationContext(): Context {
                    return activity.applicationContext
                }
            })
        } catch (e: Exception) {
            Log.e("plugin", Log.getStackTraceString(e))
        }
    }
}
