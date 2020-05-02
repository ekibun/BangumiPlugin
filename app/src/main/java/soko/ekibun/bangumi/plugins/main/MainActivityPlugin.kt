package soko.ekibun.bangumi.plugins.main

import android.annotation.SuppressLint
import android.app.Activity
import soko.ekibun.bangumi.plugins.PluginPresenter
import java.lang.ref.WeakReference

class MainActivityPlugin : PluginPresenter.Builder {
    @SuppressLint("InflateParams")
    override fun setUpPlugins(activityRef: WeakReference<Activity>) {
        MainPresenter(activityRef)
    }
}