package soko.ekibun.bangumi.plugins.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import kotlinx.android.synthetic.main.item_pull_load.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.util.ResourceUtil
import kotlin.math.sign

class PullLoadLayout constructor(context: Context, attrs: AttributeSet) : ViewGroup(context, attrs) {
    /**
     * 拖动距离
     * + 下拉刷新
     * - 上拉加载
     */
    var offset = 0

    var loading = false
        set(value) {
            field = value
            if (value) progressDrawable.start()
            else progressDrawable.stop()
        }

    val anchorDistance = ResourceUtil.dip2px(context, 36f)

    val triggerDistance = 1.5f * anchorDistance

    val touchSlop = ResourceUtil.dip2px(context, 1f)

    val isHorizontal get() = ((contentView as? RecyclerView)?.layoutManager as? LinearLayoutManager)?.orientation == RecyclerView.HORIZONTAL

    val contentView by lazy { getChildAt(0) }

    val loadView by lazy {
        val view = LayoutInflater.from(context).inflate(R.layout.item_pull_load, this, false)
        view.item_progress.setImageDrawable(progressDrawable)
        addView(view)
        view
    }

    private val progressDrawable by lazy {
        val dp = ResourceUtil.dip2px(context, 100f) / 100f
        val drawable = CircularProgressDrawable(context)
        drawable.setArrowDimensions(5 * dp, 5 * dp)
        drawable.setColorSchemeColors(ResourceUtil.resolveColorAttr(context, R.attr.colorAccent))
        drawable.strokeWidth = 2 * dp
        drawable.centerRadius = 5 * dp
        drawable
    }

    private fun updateProgress(hint: String? = null) {
        loadView.item_hint.text = hint ?: when {
            loading -> "加载中..."
            Math.abs(offset) > triggerDistance -> "释放加载"
            offset > 0 -> "加载上一章"
            else -> "加载下一章"
        }
        progressDrawable.arrowEnabled = !loading
        if (progressDrawable.arrowEnabled) {
            progressDrawable.alpha = Math.min(255, (Math.abs(offset) * 255 / (1f + anchorDistance * 2f)).toInt())
            progressDrawable.setStartEndTrim(0f, Math.min(0.75f, Math.abs(offset) / (1f + anchorDistance * 3f)))
            progressDrawable.progressRotation = offset * 0.01f
        } else {
            progressDrawable.alpha = 255
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val isHorizontal = isHorizontal
        val offsetX = l + if (isHorizontal) offset else 0
        val offsetY = t + if (isHorizontal) 0 else offset
        contentView.layout(l + offsetX, t + offsetY, r + offsetX, b + offsetY)
        loadView.visibility = if (offset == 0) View.INVISIBLE else View.VISIBLE
        if (offset == 0) return
        loadView.rotation = if (isHorizontal) -90f else 0f
        val offsetHeight = if (offset < 0) -loadView.measuredHeight else loadView.measuredHeight
        val translateX = if (isHorizontal) (offset + width) % width - (offsetHeight + loadView.measuredWidth) / 2
        else (width - loadView.measuredWidth) / 2
        val translateY = if (isHorizontal) height / 2
        else (offset + height) % height - (loadView.measuredHeight + offsetHeight) / 2
        loadView.layout(
            translateX,
            translateY,
            loadView.measuredWidth + translateX,
            loadView.measuredHeight + translateY
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun canChildScroll(direction: Int): Boolean {
        return if (isHorizontal) contentView.canScrollHorizontally(direction)
        else contentView.canScrollVertically(direction)
    }

    var hasCancel = false
    var lastTouchPos = 0
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val curTouchPos = if (isHorizontal) ev.x.toInt() else ev.y.toInt()
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchPos = curTouchPos
                hasCancel = false
                super.dispatchTouchEvent(ev)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val delta = curTouchPos - lastTouchPos
                lastTouchPos = curTouchPos
                if (Math.abs(delta) > touchSlop && !loading) {
                    val lastOffset = offset
                    offset += (delta * when {
                        offset * delta < 0 -> 1f // 反向无阻力
                        !canChildScroll(-delta) -> 0.6f // 同向检查子View能否滚动，并带上阻力
                        else -> 0f // 子View能滚动则不管
                    }).toInt()
                    if (lastOffset * offset < 0) offset = 0 // 反向置0
                    if (lastOffset != offset) {
                        if (!hasCancel) {
                            super.dispatchTouchEvent(
                                MotionEvent.obtain(
                                    ev.downTime, ev.eventTime + ViewConfiguration.getLongPressTimeout(),
                                    MotionEvent.ACTION_CANCEL, ev.x, ev.y, ev.metaState
                                )
                            )
                            hasCancel = true
                        }
                        updateProgress()
                        requestLayout()
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!loading && Math.abs(offset) > triggerDistance) {
                    loading = true
                    startAnimate()
                    updateProgress()
                    if (offset > 0) listener?.onRefresh() else listener?.onLoad()
                } else startAnimate()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    fun response(finish: Boolean) {
        if (!loading) return
        updateProgress(if (finish) "加载成功" else "加载失败")
        postDelayed({
            loading = false
            startAnimate()
        }, 750)
    }

    var animator: ValueAnimator? = null
    private fun startAnimate() {
        animator?.cancel()
        val from = offset
        val to = if (loading) anchorDistance * sign(offset.toFloat()).toInt() else 0
        if (from == to) return
        animator = ValueAnimator.ofInt(from, to)
        animator?.duration = 300
        animator?.interpolator = AccelerateDecelerateInterpolator()
        animator?.addUpdateListener {
            val lastOffset = offset
            offset = it.animatedValue as Int
            if (Math.abs(offset) < anchorDistance) {
                val delta = offset - Math.max(-anchorDistance, Math.min(anchorDistance, lastOffset))
                contentView.scrollBy(if (isHorizontal) delta else 0, if (isHorizontal) 0 else delta)
            }
            requestLayout()
        }
        animator?.start()
    }

    interface PullLoadListener {
        fun onLoad()
        fun onRefresh()
    }

    var listener: PullLoadListener? = null
}