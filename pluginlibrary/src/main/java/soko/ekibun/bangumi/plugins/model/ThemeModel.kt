package soko.ekibun.bangumi.plugins.model

import android.app.Activity
import android.content.Context
import android.view.Window
import soko.ekibun.bangumi.plugins.util.ReflectUtil


/**
 * 主题模块
 */
object ThemeModel {
    lateinit var classLoader: ClassLoader

    private val themeModel by lazy {
        ReflectUtil.proxyObject(
            classLoader.loadClass("soko.ekibun.bangumi.model.ThemeModel")
                .getDeclaredField("INSTANCE").get(null),
            IThemeModel::class.java
        )!!
    }

    fun updateNavigationTheme(activity: Activity) {
        themeModel.updateNavigationTheme(activity)
    }

    fun updateNavigationTheme(window: Window, context: Context) {
        themeModel.updateNavigationTheme(window, context)
    }

    fun fullScreen(window: Window) {
        window.decorView.systemUiVisibility = 5894
    }


    interface IThemeModel {
        fun updateNavigationTheme(activity: Activity)
        fun updateNavigationTheme(window: Window, context: Context)
    }

}