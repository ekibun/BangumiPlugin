package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.annotation.Keep

@Keep
abstract class BasePlugin : Service() {

    @Keep
    abstract fun setUpPlugins(activity: Activity, context: Context)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
