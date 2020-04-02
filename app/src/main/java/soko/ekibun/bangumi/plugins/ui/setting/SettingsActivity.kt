package soko.ekibun.bangumi.plugins.ui.setting

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import soko.ekibun.bangumi.plugins.BuildConfig
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.ui.view.BaseFragmentActivity

class SettingsActivity : BaseFragmentActivity(), PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    var lastFragment: Fragment? = null

    override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, pref: PreferenceScreen): Boolean {
        openPreferenceStartScreen(pref.key)
        title = pref.title
        return true
    }

    private fun openPreferenceStartScreen(key: String) {
        val ft = supportFragmentManager.beginTransaction()
        val fragment = SettingsFragment()
        val args = Bundle()
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key)

        fragment.arguments = args
        ft.replace(R.id.layout_content, fragment, key)
        ft.addToBackStack(key)
        ft.commit()
        lastFragment = fragment
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val fragment = supportFragmentManager.getFragment(savedInstanceState, "settings") ?: return
        val key = fragment.arguments?.getString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT) ?: ""
        lastFragment = fragment
        supportFragmentManager.beginTransaction().replace(R.id.layout_content, fragment, key).commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        lastFragment?.let {
            try {
                supportFragmentManager.putFragment(outState, "settings", it)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.getStringExtra("pref_screen_title")?.let { title = it }
    }

    override fun onViewCreated(view: View) {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0)
                setTitle(R.string.settings)
        }
        supportFragmentManager.beginTransaction().replace(
            R.id.layout_content,
            SettingsFragment()
        ).commit()
        intent?.getStringExtra("pref_screen_key")?.let { openPreferenceStartScreen(it) }
    }

    init {
        onBackListener = { supportFragmentManager.popBackStackImmediate() }
    }

    /**
     * 设置Fragment
     */
    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = super.onCreateView(inflater, container, savedInstanceState)
            val relativeLayout = RelativeLayout(layoutInflater.context)
            relativeLayout.addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return relativeLayout
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.app_preferences, rootKey)
            updatePreference()
        }

        private fun updatePreference() {
            findPreference<Preference>("check_update_now")?.summary =
                "${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE} (${BuildConfig.BUILD_TYPE})"
        }

        override fun onResume() {
            super.onResume()

            updatePreference()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        }
    }
}
