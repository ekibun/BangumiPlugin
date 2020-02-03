package soko.ekibun.bangumi.plugins.subject

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.subject_episode.view.*
import soko.ekibun.bangumi.plugins.ActivityPlugin
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.ui.provider.ProviderActivity
import soko.ekibun.bangumi.plugins.util.AppUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil
import soko.ekibun.bangumi.plugins.util.ReflectUtil
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class SubjectActivityPlugin : ActivityPlugin {
    @SuppressLint("InflateParams")
    override fun setUpPlugins(activity: Activity, context: Context) {
        LinePresenter(activity, context).refreshLines()
    }
}