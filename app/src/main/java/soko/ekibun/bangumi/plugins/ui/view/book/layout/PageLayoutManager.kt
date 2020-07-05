package soko.ekibun.bangumi.plugins.ui.view.book.layout

import android.content.Context
import android.util.DisplayMetrics
import android.view.ScaleGestureDetector
import android.view.View
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import soko.ekibun.bangumi.plugins.ui.view.SelectableRecyclerView
import soko.ekibun.bangumi.plugins.ui.view.book.ScalableLayoutManager
import kotlin.math.ceil

class PageLayoutManager(recyclerView: RecyclerView, context: Context, val rtl: Boolean = false) :
    ScalableLayoutManager(recyclerView, context) {
    var downPage = 0
    var currentPos = 0f

    /**
     * 翻页方向，滑动一次只操作一页
     * -1 上一页
     * 1 下一页
     */
    var downDirection = 0

    override var scale = 1f
        set(value) {
            field = Math.max(1f, Math.min(value, 2f))
            findViewByPosition(downPage - 1)?.translationX = width * field
        }
    var offsetY = 0

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        detachAndScrapAttachedViews(recycler)

        currentPos = Math.max(0f, Math.min(currentPos, itemCount - 1f))
        if (state.itemCount <= 0 || state.isPreLayout) return

        val currentIndex = currentPos.toInt()
        // 后一个
        val prefetchCount = 2
        for (i in prefetchCount downTo 1) if (currentIndex + i < state.itemCount) {
            val nextView = recycler.getViewForPosition(currentIndex + i)
            addView(nextView)
            nextView.translationX = 0f
            nextView.translationZ = 0f
            measureChildWithMargins(nextView, 0, 0)
            layoutDecoratedWithMargins(nextView, 0, 0, nextView.measuredWidth, nextView.measuredHeight)
        }
        val view = recycler.getViewForPosition(currentIndex)
        addView(view)
        measureChildWithMargins(view, 0, 0)
        view.translationZ = 50f
        view.translationX = (currentPos - currentIndex) * width * (if (rtl) 1 else -1)
        offsetX = Math.max(0, Math.min(view.measuredWidth - width, offsetX))
        offsetY = Math.max(0, Math.min(view.measuredHeight - height, offsetY))
        layoutDecoratedWithMargins(view, 0, 0, view.measuredWidth, view.measuredHeight)
        // 前一个
        if (currentIndex - 1 >= 0) {
            val nextView = recycler.getViewForPosition(currentIndex - 1)
            addView(nextView)
            nextView.translationX = width * scale * (if (rtl) 1 else -1)
            nextView.translationZ = 0f
            measureChildWithMargins(nextView, 0, 0)
            layoutDecoratedWithMargins(nextView, 0, 0, nextView.measuredWidth, nextView.measuredHeight)
        }
    }

    override fun measureChildWithMargins(child: View, widthUsed: Int, heightUsed: Int) {
        super.measureChildWithMargins(child, widthUsed, heightUsed)
        val lp = child.layoutParams as RecyclerView.LayoutParams
        val widthSpec = RecyclerView.LayoutManager.getChildMeasureSpec(
            (width * scale).toInt(), widthMode,
            paddingLeft + paddingRight
                    + lp.leftMargin + lp.rightMargin + widthUsed, lp.width,
            canScrollHorizontally()
        )
        if (child.measuredHeight > height * scale)
            child.measure(
                RecyclerView.LayoutManager.getChildMeasureSpec(
                    Math.max(width, (height * scale * width * scale / child.measuredHeight).toInt()), widthMode,
                    paddingLeft + paddingRight
                            + lp.leftMargin + lp.rightMargin + widthUsed, lp.width,
                    canScrollHorizontally()
                ), RecyclerView.LayoutManager.getChildMeasureSpec(
                    (height * scale).toInt(), heightMode,
                    paddingTop + paddingBottom
                            + lp.topMargin + lp.bottomMargin + heightUsed, RecyclerView.LayoutParams.MATCH_PARENT,
                    canScrollVertically()
                )
            )
        else if (child.measuredHeight < height)
            child.measure(
                widthSpec, RecyclerView.LayoutManager.getChildMeasureSpec(
                    height, heightMode,
                    paddingTop + paddingBottom
                            + lp.topMargin + lp.bottomMargin + heightUsed, RecyclerView.LayoutParams.MATCH_PARENT,
                    canScrollVertically()
                )
            )
    }

    override fun layoutDecoratedWithMargins(child: View, left: Int, top: Int, right: Int, bottom: Int) {
        super.layoutDecoratedWithMargins(child, left, top - offsetY, right, bottom - offsetY)
    }

    override fun computeHorizontalScrollOffset(state: RecyclerView.State): Int {
        return ((if (rtl) itemCount - currentPos - 1 else currentPos) * width).toInt() + if (scale > 1f) 1 else 0
    }

    override fun computeHorizontalScrollRange(state: RecyclerView.State): Int {
        return (itemCount * width * scale).toInt()
    }

    override fun onScaleBegin(detector: ScaleGestureDetector?) {
        currentPos = Math.round(currentPos).toFloat()
        requestLayout()
    }

    override fun onTouchDown() {
        if (Math.abs(currentPos - Math.round(currentPos)) * width < 5)
            currentPos = Math.round(currentPos).toFloat()
        downDirection = if (currentPos - currentPos.toInt() > 0) 1 else 0
        downPage = currentPos.toInt()
    }

    override fun scrollToPositionWithOffset(position: Int, offset: Int) {
        currentPos = position.toFloat()
        downPage = position
        super.scrollToPositionWithOffset(position, offset)
    }

    override fun reset() {
        super.reset()
        offsetY = 0
    }

    override fun findFirstVisibleItemPosition(): Int {
        return currentPos.toInt()
    }

    override fun findLastVisibleItemPosition(): Int {
        return currentPos.toInt() + if (currentPos - currentPos.toInt() != 0f) 1 else 0
    }

    override fun findFirstCompletelyVisibleItemPosition(): Int {
        return currentPos.toInt()
    }

    override fun findLastCompletelyVisibleItemPosition(): Int {
        return currentPos.toInt()
    }

    override fun setOrientation(orientation: Int) {
        super.setOrientation(HORIZONTAL)
    }

    override fun onFling(velocityX: Int, velocityY: Int): Boolean {
        val minFlingVelocity = recyclerView.minFlingVelocity
        if (orientation == VERTICAL || scale > 1f) return false

        val targetPos = when {
            Math.abs(velocityX) < minFlingVelocity -> Math.round(currentPos)
            velocityX * (if (rtl) -1 else 1) < 0 -> currentPos.toInt()
            else -> Math.min(currentPos.toInt() + 1, itemCount - 1)
        }
        snapToTarget(targetPos)

        return true
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (scale <= 1f && state == RecyclerView.SCROLL_STATE_IDLE) {
            snapToTarget(Math.round(currentPos))
        }
    }

    override fun scrollOnScale(x: Float, y: Float, oldScale: Float) {
        recyclerView.scrollBy(
            if (scale == 1f) 0 else ((offsetX + x) * (scale - oldScale) / oldScale).toInt(),
            ((offsetY + y) * (scale - oldScale) / oldScale).toInt()
        )
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State?): Int {
        val view = findViewByPosition(currentPos.toInt())
        val ddy = Math.max(Math.min(dy, (view?.height ?: height) - height - offsetY), -offsetY)
        offsetY += ddy
        offsetChildrenVertical(-ddy)
        return if (scale == 1f) dy else ddy
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        val view = findViewByPosition(downPage)
        val ddx = Math.max(Math.min(dx, (view?.width ?: width) - width - offsetX), -offsetX)
        offsetX += ddx
        offsetChildrenHorizontal(-ddx)
        view?.translationX = 0f
        updateContent()
        if (scale > 1 || doingScale || view == null) return if (scale == 1f) dx else ddx

        if (downDirection == 0) downDirection = dx * (if (rtl) -1 else 1)
        currentPos = Math.max(
            downPage - if (downDirection > 0) 0f else 1f,
            Math.min(
                currentPos + (dx * (if (rtl) -1 else 1)).toFloat() / width,
                downPage + if (downDirection < 0) 0f else 1f
            )
        )
        currentPos = Math.max(0f, Math.min(currentPos, itemCount - 1f))
        view.translationX = Math.max((currentPos - downPage) * width, 0f) * (if (rtl) 1 else -1)
        val prevPage = findViewByPosition(downPage - 1)
        if (currentPos < downPage) findViewByPosition(downPage - 1)?.translationX =
            (currentPos - downPage + 1) * width * (if (rtl) 1 else -1)
        if ((ceil(currentPos) - currentPos) * width < 5) {
            prevPage?.translationZ = 0f
            view.translationZ = 0f
        } else {
            view.translationZ = 50f
            if (currentPos < downPage) prevPage?.translationZ = 100f
        }

        (recyclerView as? SelectableRecyclerView)?.clearSelect()
        return dx
    }

    fun snapToTarget(targetPos: Int) {
        if (targetPos < 0 || targetPos > itemCount - 1) return
        val smoothScroller: LinearSmoothScroller = createSnapScroller(targetPos)
        smoothScroller.targetPosition = targetPos
        startSmoothScroll(smoothScroller)
    }

    private fun createSnapScroller(targetPos: Int): LinearSmoothScroller {
        return object : LinearSmoothScroller(recyclerView.context) {
            override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
                val dx = ((currentPos - targetPos) * (width + 1f)).toInt() * (if (rtl) 1 else -1)
                val time = calculateTimeForDeceleration(Math.abs(dx))
                if (time > 0) {
                    action.update(dx, 0, time, mDecelerateInterpolator)
                }
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
            }

            override fun calculateTimeForScrolling(dx: Int): Int {
                return Math.min(
                    MAX_SCROLL_ON_FLING_DURATION,
                    super.calculateTimeForScrolling(dx)
                )
            }
        }
    }

    companion object {
        const val MILLISECONDS_PER_INCH = 100f
        const val MAX_SCROLL_ON_FLING_DURATION = 100 // ms
    }
}