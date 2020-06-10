package soko.ekibun.bangumi.plugins.ui.view.book.layout

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import soko.ekibun.bangumi.plugins.ui.view.book.ScalableLayoutManager

class RollLayoutManager(recyclerView: RecyclerView, context: Context) : ScalableLayoutManager(recyclerView, context) {
    override fun scrollOnScale(x: Float, y: Float, oldScale: Float) {
        recyclerView.scrollBy(
            ((offsetX + x) * (scale - oldScale) / oldScale).toInt(),
            findViewByPosition(anchorPos ?: return)?.let {
                ((y - getDecoratedTop(it)) * (scale - oldScale) / oldScale).toInt()
            } ?: 0
        )
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        val ddx = Math.max(Math.min(dx, (width * scale).toInt() - width - offsetX), -offsetX)
        offsetX += ddx
        offsetChildrenHorizontal(-ddx)
        updateContent()
        return if (scale == 1f) dx else ddx
    }

    override fun setOrientation(orientation: Int) {
        super.setOrientation(VERTICAL)
    }
}