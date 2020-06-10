package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.annotation.Keep
import soko.ekibun.bangumi.plugins.model.ThemeModel

@Keep
abstract class BasePlugin : Service() {

    @Keep
    open fun setUpPlugins(activity: Activity, context: Context) {
        ThemeModel.classLoader = activity.classLoader
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
