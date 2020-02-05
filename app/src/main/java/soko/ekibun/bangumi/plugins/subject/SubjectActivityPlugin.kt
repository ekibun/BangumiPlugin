package soko.ekibun.bangumi.plugins.subject

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import soko.ekibun.bangumi.plugins.ActivityPlugin

class SubjectActivityPlugin : ActivityPlugin {
    @SuppressLint("InflateParams")
    override fun setUpPlugins(activity: Activity, context: Context) {
        LinePresenter(activity, context).refreshLines()
    }
}