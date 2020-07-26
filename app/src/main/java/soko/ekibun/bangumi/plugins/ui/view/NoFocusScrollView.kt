package soko.ekibun.bangumi.plugins.ui.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

class NoFocusScrollView constructor(context: Context, attrs: AttributeSet) : NestedScrollView(context, attrs) {
    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect?): Int {
        return 0
    }
}