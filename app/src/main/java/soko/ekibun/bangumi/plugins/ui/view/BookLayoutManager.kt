package soko.ekibun.bangumi.plugins.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView


class BookLayoutManager(val context: Context, val updateContent: (View, BookLayoutManager) -> Unit) :
    LinearLayoutManager(context) {
    var scale = 1f
        set(value) {
            field = Math.max(1f, Math.min(value, 2f))
            if (orientation == HORIZONTAL)
                findViewByPosition(downPage - 1)?.translationX = width * field
        }
    var offsetX = 0
    var offsetY = 0

    fun reset() {
        scale = 1f
        offsetX = 0
        offsetY = 0
    }

    interface ScalableAdapter {
        fun isItemScalable(pos: Int, layoutManager: LinearLayoutManager): Boolean
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (orientation == VERTICAL) return super.onLayoutChildren(recycler, state)
        detachAndScrapAttachedViews(recycler)

        currentPos = Math.max(0f, Math.min(currentPos, itemCount - 1f))
        if (state.itemCount <= 0 || state.isPreLayout) return

        val currentIndex = currentPos.toInt()
        val view = recycler.getViewForPosition(currentIndex)
        addView(view)
        measureChildWithMargins(view, 0, 0)
        view.translationZ = 50f
        view.translationX = -(currentPos - currentIndex) * width
        layoutDecoratedWithMargins(view, 0, 0, view.measuredWidth, view.measuredHeight)
        // 前一个
        if (currentIndex - 1 >= 0) {
            val nextView = recycler.getViewForPosition(currentIndex - 1)
            addView(nextView)
            nextView.translationX = -width * scale
            nextView.translationZ = 100f
            measureChildWithMargins(nextView, 0, 0)
            layoutDecoratedWithMargins(nextView, 0, 0, view.measuredWidth, view.measuredHeight)
        }
        // 后一个
        if (currentIndex + 1 < state.itemCount) {
            val nextView = recycler.getViewForPosition(currentIndex + 1)
            addView(nextView)
            nextView.translationX = 0f
            nextView.translationZ = 0f
            measureChildWithMargins(nextView, 0, 0)
            layoutDecoratedWithMargins(nextView, 0, 0, view.measuredWidth, view.measuredHeight)
        }
    }

    fun scrollOnScale(x: Float, y: Float, oldScale: Float) {
        val adapter = recyclerView.adapter
        val anchorPos = (if (adapter is ScalableAdapter) {
            (findFirstVisibleItemPosition()..findLastVisibleItemPosition()).firstOrNull {
                adapter.isItemScalable(it, this)
            } ?: {
                scale = 1f
                null
            }()
        } else findFirstVisibleItemPosition()) ?: return
        recyclerView.scrollBy(
            if (scale == 1f && orientation == HORIZONTAL) 0 else ((offsetX + x) * (scale - oldScale) / oldScale).toInt(),
            if (orientation == HORIZONTAL) ((offsetY + y) * (scale - oldScale) / oldScale).toInt() else 0
        )
        if (orientation == VERTICAL) findViewByPosition(anchorPos)?.let {
            scrollToPositionWithOffset(anchorPos, (y - (-getDecoratedTop(it) + y) * scale / oldScale).toInt())
        }
    }

    override fun findFirstVisibleItemPosition(): Int {
        return if (orientation == VERTICAL) super.findFirstVisibleItemPosition() else currentPos.toInt()
    }

    override fun findLastVisibleItemPosition(): Int {
        return if (orientation == VERTICAL) super.findLastVisibleItemPosition() else currentPos.toInt()
    }

    override fun findFirstCompletelyVisibleItemPosition(): Int {
        return if (orientation == VERTICAL) super.findFirstCompletelyVisibleItemPosition() else currentPos.toInt()
    }

    override fun findLastCompletelyVisibleItemPosition(): Int {
        return if (orientation == VERTICAL) super.findLastCompletelyVisibleItemPosition() else currentPos.toInt()
    }

    var currentPos = 0f
    override fun scrollToPositionWithOffset(position: Int, offset: Int) {
        currentPos = position.toFloat()
        downPage = position
        super.scrollToPositionWithOffset(position, offset)
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
        val heightSpec = RecyclerView.LayoutManager.getChildMeasureSpec(
            height, heightMode,
            paddingTop + paddingBottom
                    + lp.topMargin + lp.bottomMargin + heightUsed, lp.height,
            canScrollVertically()
        )
        child.measure(widthSpec, heightSpec)
        if (orientation == VERTICAL) return
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
        updateContent(child, this)
        if (orientation == VERTICAL) {
            child.translationX = 0f
            child.translationZ = 0f
        }
        offsetX = Math.max(0, Math.min(right - left - width, offsetX))
        offsetY = if (orientation == VERTICAL) 0 else Math.max(0, Math.min(bottom - top - height, offsetY))
        super.layoutDecoratedWithMargins(child, left - offsetX, top - offsetY, right - offsetX, bottom - offsetY)
    }


    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State?): Int {
        if (orientation == VERTICAL) return super.scrollVerticallyBy(dy, recycler, state)

        val view = findViewByPosition(currentPos.toInt())
        val ddy = Math.max(Math.min(dy, (view?.height ?: height) - height - offsetY), -offsetY)
        offsetY += ddy
        offsetChildrenVertical(-ddy)
        return if (scale == 1f) dy else ddy
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        val view = findViewByPosition(downPage)
        val ddx = Math.max(
            Math.min(
                dx,
                (if (orientation == VERTICAL) (width * scale).toInt() else view?.width ?: width) - width - offsetX
            ), -offsetX
        )
        offsetX += ddx
        offsetChildrenHorizontal(-ddx)
        view?.translationX = 0f
        for (i in 0 until recyclerView.childCount) updateContent(recyclerView.getChildAt(i), this)

        if (orientation == VERTICAL || scale > 1 || doingScale || view == null) return if (scale == 1f) dx else ddx

        if (downDirection == 0) downDirection = dx
        currentPos = Math.max(
            downPage - if (downDirection > 0) 0f else 1f,
            Math.min(currentPos + dx.toFloat() / width, downPage + if (downDirection < 0) 0f else 1f)
        )
        currentPos = Math.max(0f, Math.min(currentPos, itemCount - 1f))
        view.translationX = -Math.max((currentPos - downPage) * width, 0f)
        if (currentPos < downPage) findViewByPosition(downPage - 1)?.translationX = -(currentPos - downPage + 1) * width
//        if(lastPos != currentPos.toInt())
        return dx
    }

    override fun computeHorizontalScrollOffset(state: RecyclerView.State): Int {
        return if (orientation == VERTICAL) super.computeHorizontalScrollOffset(state)
        else (currentPos * width).toInt() + if (scale > 1f) 1 else 0
    }

    override fun computeHorizontalScrollRange(state: RecyclerView.State): Int {
        return if (orientation == VERTICAL) super.computeHorizontalScrollRange(state)
        else itemCount * width
    }

    var downPage = 0

    /**
     * 翻页方向，滑动一次只操作一页
     * -1 上一页
     * 1 下一页
     */
    var downDirection = 0

    override fun canScrollHorizontally(): Boolean = true
    override fun canScrollVertically(): Boolean = true

    var doingScale = false
    lateinit var recyclerView: RecyclerView

    @SuppressLint("ClickableViewAccessibility")
    fun setupWithRecyclerView(
        view: RecyclerView,
        onTap: (Int, Int) -> Unit,
        onPress: (View, Int) -> Unit,
        onTouch: (MotionEvent) -> Unit
    ) {
        recyclerView = view
        view.layoutManager = this
        var beginScale = scale
        val scaleGestureDetector =
            ScaleGestureDetector(view.context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
                    beginScale = scale
                    currentPos = Math.round(currentPos).toFloat()
                    doingScale = true
                    requestLayout()
                    return super.onScaleBegin(detector)
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val oldScale = scale
                    scale = beginScale * detector.scaleFactor
                    scrollOnScale(detector.focusX, detector.focusY, oldScale)
                    requestLayout()
                    return super.onScale(detector)
                }
            })
        val gestureDetector = GestureDetectorCompat(view.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onTap((e.x).toInt(), (e.y).toInt())
                return super.onSingleTapConfirmed(e)
            }

            override fun onLongPress(e: MotionEvent) {
                view.findChildViewUnder(e.x, e.y)?.let { onPress(it, view.getChildAdapterPosition(it)) }
                super.onLongPress(e)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val oldScale = scale
                scale = if (scale < 2f) 2f else 1f
                scrollOnScale(e.x, e.y, oldScale)
                requestLayout()
                return super.onDoubleTap(e)
            }
        })
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (scale * width < 10) scale = 1f
                    doingScale = false
                    if (Math.abs(currentPos - Math.round(currentPos)) * width < 5)
                        currentPos = Math.round(currentPos).toFloat()
                    downDirection = if (currentPos - currentPos.toInt() > 0) 1 else 0
                    downPage = currentPos.toInt()
                    requestLayout()
                }
            }

            onTouch(event)
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            false
        }
        view.onFlingListener = object : RecyclerView.OnFlingListener() {
            override fun onFling(velocityX: Int, velocityY: Int): Boolean {
                val minFlingVelocity = recyclerView.minFlingVelocity
                if (orientation == VERTICAL || scale > 1f) return false

                val targetPos = when {
                    Math.abs(velocityX) < minFlingVelocity -> Math.round(currentPos)
                    velocityX < 0 -> currentPos.toInt()
                    else -> Math.min(currentPos.toInt() + 1, itemCount - 1)
                }
                snapToTarget(targetPos)

                return true
            }
        }
        view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (orientation == VERTICAL || scale > 1f) return
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    snapToTarget(Math.round(currentPos))
                }
            }
        })
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
                val dx = -((currentPos - targetPos) * (width + 0.5f)).toInt()
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