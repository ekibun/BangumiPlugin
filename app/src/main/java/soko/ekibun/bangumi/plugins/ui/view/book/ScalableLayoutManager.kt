package soko.ekibun.bangumi.plugins.ui.view.book

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


abstract class ScalableLayoutManager(val recyclerView: RecyclerView, val context: Context) :
    LinearLayoutManager(context) {
    open var scale = 1f
        set(value) {
            field = Math.max(1f, Math.min(value, 2f))
        }
    var offsetX = 0

    open fun reset() {
        scale = 1f
        offsetX = 0
    }

    open fun onFling(velocityX: Int, velocityY: Int): Boolean {
        return false
    }

    open fun onScaleBegin(detector: ScaleGestureDetector?) {}

    open fun onTouchDown() {}

    val anchorPos: Int?
        get() {
            val adapter = recyclerView.adapter
            return if (adapter is ScalableAdapter) {
                (findFirstVisibleItemPosition()..findLastVisibleItemPosition()).firstOrNull {
                    adapter.isItemScalable(it, this)
                } ?: {
                    scale = 1f
                    null
                }()
            } else findFirstVisibleItemPosition()
        }

    interface ScalableAdapter {
        fun isItemScalable(pos: Int, layoutManager: ScalableLayoutManager): Boolean
        fun updateContent(view: View, layoutManager: ScalableLayoutManager): Unit
    }

    abstract fun scrollOnScale(x: Float, y: Float, oldScale: Float)

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
//        if (orientation == VERTICAL) return
//        if (child.measuredHeight > height * scale)
//            child.measure(
//                RecyclerView.LayoutManager.getChildMeasureSpec(
//                    Math.max(width, (height * scale * width * scale / child.measuredHeight).toInt()), widthMode,
//                    paddingLeft + paddingRight
//                            + lp.leftMargin + lp.rightMargin + widthUsed, lp.width,
//                    canScrollHorizontally()
//                ), RecyclerView.LayoutManager.getChildMeasureSpec(
//                    (height * scale).toInt(), heightMode,
//                    paddingTop + paddingBottom
//                            + lp.topMargin + lp.bottomMargin + heightUsed, RecyclerView.LayoutParams.MATCH_PARENT,
//                    canScrollVertically()
//                )
//            )
//        else if (child.measuredHeight < height)
//            child.measure(
//                widthSpec, RecyclerView.LayoutManager.getChildMeasureSpec(
//                    height, heightMode,
//                    paddingTop + paddingBottom
//                            + lp.topMargin + lp.bottomMargin + heightUsed, RecyclerView.LayoutParams.MATCH_PARENT,
//                    canScrollVertically()
//                )
//            )
    }

    fun updateContent() {
        val adapter = recyclerView.adapter as? ScalableAdapter ?: return
        for (i in 0 until recyclerView.childCount) adapter.updateContent(recyclerView.getChildAt(i), this)
    }

    override fun layoutDecoratedWithMargins(child: View, left: Int, top: Int, right: Int, bottom: Int) {
        (recyclerView.adapter as? ScalableAdapter)?.updateContent(child, this)
//        if (orientation == VERTICAL) {
//            child.translationX = 0f
//            child.translationZ = 0f
//        }
        offsetX = Math.max(0, Math.min(right - left - width, offsetX))
        super.layoutDecoratedWithMargins(child, left - offsetX, top, right - offsetX, bottom)
    }

    override fun canScrollHorizontally(): Boolean = true
    override fun canScrollVertically(): Boolean = true

    var doingScale = false

    companion object {
        fun setupWithRecyclerView(
            view: RecyclerView,
            touchListener: GestureTouchListener
        ) {
//            recyclerView = view
//            view.layoutManager = this
            var beginScale = 0f
            touchListener.listeners.add(object : GestureTouchListener.GestureCallback() {
                override fun onScaleBegin(detector: ScaleGestureDetector?) {
                    val layoutManager = (view.layoutManager as? ScalableLayoutManager) ?: return
                    beginScale = layoutManager.scale
                    layoutManager.doingScale = true
                    layoutManager.onScaleBegin(detector)
                }

                override fun onScale(detector: ScaleGestureDetector) {
                    val layoutManager = (view.layoutManager as? ScalableLayoutManager) ?: return
                    val oldScale = layoutManager.scale
                    layoutManager.scale = beginScale * detector.scaleFactor
                    layoutManager.scrollOnScale(detector.focusX, detector.focusY, oldScale)
                    layoutManager.requestLayout()
                }

                override fun onDoubleTap(e: MotionEvent) {
                    val layoutManager = (view.layoutManager as? ScalableLayoutManager) ?: return
                    val oldScale = layoutManager.scale
                    layoutManager.scale = if (layoutManager.scale < 2f) 2f else 1f
                    view.post { layoutManager.scrollOnScale(e.x, e.y, oldScale) }
                    layoutManager.requestLayout()
                }

                override fun onTouch(e: MotionEvent) {
                    when (e.action) {
                        MotionEvent.ACTION_DOWN -> {
                            val layoutManager = (view.layoutManager as? ScalableLayoutManager) ?: return
                            if ((layoutManager.scale - 1) * layoutManager.width < 10) layoutManager.scale = 1f
                            layoutManager.doingScale = false
                            layoutManager.onTouchDown()
                            layoutManager.requestLayout()
                        }
                    }
                }
            })

            view.onFlingListener = object : RecyclerView.OnFlingListener() {
                override fun onFling(velocityX: Int, velocityY: Int): Boolean {
                    return (view.layoutManager as? ScalableLayoutManager)?.onFling(velocityX, velocityY) ?: false
                }
            }
        }
    }
}