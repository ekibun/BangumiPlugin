package soko.ekibun.bangumi.plugins.main

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import soko.ekibun.bangumi.plugins.bean.Subject

interface IMainActivity {
    val nav_view: View
    val content_frame: FrameLayout
    val mainPresenter: IMainPresenter

    var onBackListener: () -> Boolean

    interface INavigationView {
        val menu: Menu
        fun setCheckedItem(id: Int)
    }

    interface IMainPresenter {
        val drawerView: IDrawerView
        var mergeAirInfo: () -> Unit
        var collectionList: List<*>
        fun notifyCollectionChange()

        interface ISubject {
            val id: Int
            @Subject.SubjectType
            var type: String
            var airInfo: String?
        }
    }

    interface IDrawerView {
        var navigationItemSelectedListener: (MenuItem) -> Boolean
        fun select(id: Int)
    }
}