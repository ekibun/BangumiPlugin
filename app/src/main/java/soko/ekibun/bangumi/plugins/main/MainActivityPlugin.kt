package soko.ekibun.bangumi.plugins.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import soko.ekibun.bangumi.plugins.ActivityPlugin

class MainActivityPlugin : ActivityPlugin {
    @SuppressLint("InflateParams")
    override fun setUpPlugins(activity: Activity, context: Context) {
        MainPresenter(activity, context)
    }
}