package soko.ekibun.bangumi.plugins.subject

import android.app.Activity
import soko.ekibun.bangumi.plugins.ActivityPlugin
import java.lang.ref.WeakReference

class SubjectActivityPlugin : ActivityPlugin {

    override fun setUpPlugins(activityRef: WeakReference<Activity>) {
        LinePresenter(activityRef).refreshLines()
    }
}