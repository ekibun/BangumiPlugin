package soko.ekibun.bangumi.plugins.ui.view.book

import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
abstract class SelectableActionMode : ActionMode.Callback2() {

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.title = null
        mode.subtitle = null
        mode.titleOptionalHint = true
        populateMenuWithItems(menu)
        return true
    }

    private fun populateMenuWithItems(menu: Menu) {
        menu.add(
            Menu.NONE, ID_COPY, MENU_ITEM_ORDER_COPY,
            "复制"
        ).setAlphabeticShortcut('c').setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(
            Menu.NONE, ID_SHARE, MENU_ITEM_ORDER_SHARE,
            "分享"
        ).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
    }

    companion object {
        const val ID_COPY = android.R.id.copy
        const val ID_SHARE = android.R.id.shareText
        private const val MENU_ITEM_ORDER_COPY = 5
        private const val MENU_ITEM_ORDER_SHARE = 7
    }
}