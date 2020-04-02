package soko.ekibun.bangumi.plugins.ui.view

import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import soko.ekibun.bangumi.plugins.model.ThemeModel

/**
 * 基础Activity
 * @property resId Int
 * @constructor
 */
abstract class BaseActivity(@LayoutRes private val resId: Int) : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(resId)
        ThemeModel.updateNavigationTheme(this.window, this)
    }

    var onBackListener = { false }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> if (!onBackListener()) finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!onBackListener()) finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}