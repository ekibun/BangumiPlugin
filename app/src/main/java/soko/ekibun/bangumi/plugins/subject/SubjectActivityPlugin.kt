package soko.ekibun.bangumi.plugins.subject

import android.app.Activity
import soko.ekibun.bangumi.plugins.PluginPresenter
import java.lang.ref.WeakReference

class SubjectActivityPlugin : PluginPresenter.Builder {

    override fun setUpPlugins(activityRef: WeakReference<Activity>) {
        LinePresenter(activityRef).refreshLines()
    }
}