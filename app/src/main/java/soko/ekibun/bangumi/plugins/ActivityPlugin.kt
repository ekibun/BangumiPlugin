package soko.ekibun.bangumi.plugins

import android.app.Activity
import android.content.Context

interface ActivityPlugin {
    fun setUpPlugins(activity: Activity, context: Context)
}