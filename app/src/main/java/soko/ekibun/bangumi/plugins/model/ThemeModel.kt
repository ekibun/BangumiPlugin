package soko.ekibun.bangumi.plugins.model

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import soko.ekibun.bangumi.plugins.util.ResourceUtil

/**
 * 主题模块
 */
object ThemeModel {
    /**
     * 应用导航栏主题
     */
    fun updateNavigationTheme(window: Window, context: Context) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                (if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION else 0)
        if (Build.VERSION.SDK_INT < 26) return
        val light = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO
        if (light) window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        val color = ResourceUtil.resolveColorAttr(context, android.R.attr.colorBackground)
        window.navigationBarColor = Color.argb(200, Color.red(color), Color.green(color), Color.blue(color))
    }
}