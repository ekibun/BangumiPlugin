package soko.ekibun.bangumi.plugins.main

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout

interface IMainActivity {
    val nav_view: View
    val content_frame: FrameLayout
    val mainPresenter: IMainPresenter

    interface INavigationView {
        val menu: Menu
        fun setCheckedItem(id: Int)
    }

    interface IMainPresenter {
        val drawerView: IDrawerView
    }

    interface IDrawerView {
        var navigationItemSelectedListener: (MenuItem) -> Boolean
    }
}