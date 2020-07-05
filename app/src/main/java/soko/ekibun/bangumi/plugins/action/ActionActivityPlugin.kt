package soko.ekibun.bangumi.plugins.action

import android.annotation.SuppressLint
import android.app.Activity
import soko.ekibun.bangumi.plugins.PluginPresenter
import java.lang.ref.WeakReference

class ActionActivityPlugin : PluginPresenter.Builder {
    @SuppressLint("InflateParams")
    override fun setUpPlugins(activityRef: WeakReference<Activity>) {
        ActionPresenter(activityRef)
    }
}