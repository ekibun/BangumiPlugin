package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.appcompat.view.ContextThemeWrapper
import com.pl.sphelper.SPHelper
import soko.ekibun.bangumi.plugins.subject.SubjectActivityPlugin

@Keep
object Plugin {
    private val pluginList = mapOf<String, ActivityPlugin>(
        "soko.ekibun.bangumi.ui.subject.SubjectActivity" to SubjectActivityPlugin()
    )

    @Keep
    fun setUpPlugins(activity: Activity, context: Context) {
        SPHelper.init(context)
        Log.v("plugin", activity.javaClass.name)
        try {
            val themeContext = object: ContextThemeWrapper(context, R.style.AppTheme){
                override fun getApplicationContext(): Context { return context }
            }
            themeContext.applyOverrideConfiguration(activity.resources.configuration)

            pluginList[activity.javaClass.name]?.setUpPlugins(activity, themeContext)
        } catch (e: Exception) {
            Log.e("plugin", Log.getStackTraceString(e))
        }
    }
}
