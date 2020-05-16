package soko.ekibun.bangumi.plugins.ui.view

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.ActionMode
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.ui.view.book.BookLayoutManager
import soko.ekibun.bangumi.plugins.ui.view.book.SelectableActionMode
import soko.ekibun.bangumi.plugins.util.AppUtil
import soko.ekibun.bangumi.plugins.util.ResourceUtil
import kotlin.math.roundToInt

class SelectableRecyclerView constructor(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
    val bookAdapter get() = adapter as BaseQuickAdapter<*, *>
    val textSelectionAdapter get() = adapter as TextSelectionAdapter
    val bookLayoutManager get() = layoutManager as BookLayoutManager

    var actionMode: ActionMode? = null

    val selectionHandleLeft = ResourceUtil.resolveDrawableAttr(context, android.R.attr.textSelectHandleLeft)?.also {
        val delta = (it.intrinsicWidth - it.intrinsicHeight) / 2
        it.setBounds(-it.intrinsicWidth + delta, 0, delta, it.intrinsicHeight)
    }
    val selectionHandleRight = ResourceUtil.resolveDrawableAttr(context, android.R.attr.textSelectHandleRight)?.also {
        val delta = (it.intrinsicWidth - it.intrinsicHeight) / 2
        it.setBounds(-delta, 0, it.intrinsicWidth - delta, it.intrinsicHeight)
    }

    interface TextSelectionAdapter {
        fun drawSelection(c: Canvas, view: View, start: Int?, end: Int?, paint: Paint)

        fun getPosFromPosition(view: View, x: Float, y: Float): Int

        fun getHandlePosition(view: View, offset: Int): Point

        fun getTextHeight(): Int

        fun getSelectionText(startIndex: Int, endIndex: Int, startPos: Int, endPos: Int): String
    }

    var isActive = false

    data class SelectItem(
        val item: Any,
        val pos: Int
    )

    var selectStart: SelectItem? = null
    var selectEnd: SelectItem? = null

    val paint = Paint().also {
        it.color = ResourceUtil.resolveColorAttr(context, R.attr.colorAccent)
        it.alpha = 96
    }

    init {
        addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun onDrawOver(c: Canvas, parent: RecyclerView, state: State) {
                super.onDrawOver(c, parent, state)
                val (startPair, endPair) = getSelectRange() ?: return
                val (startIndex, selectStart) = startPair
                val (endIndex, selectEnd) = endPair
                val firstVisibleSelectPos = Math.max(startIndex, bookLayoutManager.findFirstVisibleItemPosition())
                val lastVisibleSelectPos = Math.min(endIndex, bookLayoutManager.findLastVisibleItemPosition())
                for (pos in firstVisibleSelectPos..lastVisibleSelectPos) {
                    textSelectionAdapter.drawSelection(
                        c, bookLayoutManager.findViewByPosition(pos) ?: continue,
                        if (pos == startIndex) selectStart.pos else null,
                        if (pos == endIndex) selectEnd.pos else null, paint
                    )
                }
                bookLayoutManager.findViewByPosition(startIndex)?.let {
                    val pos = textSelectionAdapter.getHandlePosition(it, selectStart.pos)
                    c.save()
                    c.translate(pos.x.toFloat(), pos.y.toFloat())
                    selectionHandleLeft?.draw(c)
                    c.restore()
                }
                bookLayoutManager.findViewByPosition(endIndex)?.let {
                    val pos = textSelectionAdapter.getHandlePosition(it, selectEnd.pos)
                    c.save()
                    c.translate(pos.x.toFloat(), pos.y.toFloat())
                    selectionHandleRight?.draw(c)
                    c.restore()
                }
            }
        })
    }

    override fun canScrollVertically(direction: Int): Boolean {
        return isActive || super.canScrollVertically(direction)
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        return isActive || super.canScrollHorizontally(direction)
    }

    var handleOffset = 0
    private fun checkTouchHandle(e: MotionEvent): Boolean {
        val (startPair, endPair) = getSelectRange() ?: return false
        val (startIndex, selectStart) = startPair
        val (endIndex, selectEnd) = endPair
        val textHeight = textSelectionAdapter.getTextHeight()
        bookLayoutManager.findViewByPosition(startIndex)?.let {
            val bounds = selectionHandleLeft?.bounds ?: return false
            val pos = textSelectionAdapter.getHandlePosition(it, selectStart.pos)
            if (Rect(
                    pos.x + bounds.left,
                    pos.y + bounds.top - textHeight,
                    pos.x + bounds.right,
                    pos.y + bounds.bottom + bounds.height() / 2
                ).contains(e.x.toInt(), e.y.toInt())
            ) {
                this.selectStart = selectEnd
                this.selectEnd = selectStart
                handleOffset = (e.y - pos.y).roundToInt() + textHeight / 2
                return true
            }
        }
        bookLayoutManager.findViewByPosition(endIndex)?.let {
            val bounds = selectionHandleRight?.bounds ?: return false
            val pos = textSelectionAdapter.getHandlePosition(it, selectEnd.pos)
            if (Rect(
                    pos.x + bounds.left,
                    pos.y + bounds.top - textHeight,
                    pos.x + bounds.right,
                    pos.y + bounds.bottom + bounds.height() / 2
                ).contains(e.x.toInt(), e.y.toInt())
            ) {
                this.selectStart = selectStart
                this.selectEnd = selectEnd
                handleOffset = (e.y - pos.y).roundToInt() + textHeight / 2
                return true
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!isActive && e.actionMasked == MotionEvent.ACTION_DOWN) {
            isActive = checkTouchHandle(e)
            hideActionMode()
        }
        if (!isActive) return super.onTouchEvent(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if ((layoutManager as? BookLayoutManager)?.orientation != LinearLayoutManager.VERTICAL
                    || (!inTopSpot && !inBottomSpot)
                ) //更新滑动选择区域
                    updateSelectedRange(e.x, e.y)
                //在顶部或者底部触发自动滑动
                processAutoScroll(e)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                //结束滑动选择，初始化各状态值
                reset()
                showActionMode()
            }

        }
        return true
    }

    fun clearSelect(): Boolean {
        val ret = selectStart != null
        selectStart = null
        selectEnd = null
        reset()
        hideActionMode()
        if (ret) postInvalidate()
        return ret
    }

    private fun reset() {
        isActive = false
        lastX = java.lang.Float.MIN_VALUE
        lastY = java.lang.Float.MIN_VALUE
        stopAutoScroll()
    }

    fun startSelect(x: Float, y: Float) {
        clearSelect()
        handleOffset = 0
        updateSelectedRange(x, y, true)
    }

    private fun updateSelectedRange(x: Float, y: Float, isStart: Boolean = false) {
        val child = findChildViewUnder(x, y - handleOffset) ?: return
        val position = getChildAdapterPosition(child)
        if (position == NO_POSITION) return
        selectEnd = bookAdapter.data.getOrNull(position)?.let {
            SelectItem(it, textSelectionAdapter.getPosFromPosition(child, x, y - handleOffset))
        }?.also {
            if (!isStart) return@also
            selectStart = it
            isActive = true
        }
        postInvalidate()
    }

    private var inTopSpot: Boolean = false
    private var inBottomSpot: Boolean = false

    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var scrollDistance: Int = 0

    private fun scrollBy(distance: Int) {
        val scrollDistance: Int = if (distance > 0) {
            Math.min(distance, MAX_SCROLL_DISTANCE)
        } else {
            Math.max(distance, -MAX_SCROLL_DISTANCE)
        }
        scrollBy(0, scrollDistance)
        if (lastX != java.lang.Float.MIN_VALUE && lastY != java.lang.Float.MIN_VALUE) {
            updateSelectedRange(lastX, lastY)
        }
    }

    private fun stopAutoScroll() {
        removeCallbacks(scrollRun)
    }

    private val scrollRun = object : Runnable {
        override fun run() {
            scrollBy(scrollDistance)
            ViewCompat.postOnAnimation(this@SelectableRecyclerView, this)
        }
    }

    private fun startAutoScroll() {
        removeCallbacks(scrollRun)
        ViewCompat.postOnAnimation(this, scrollRun)
    }

    private val autoScrollDistance = (Resources.getSystem().displayMetrics.density * 56).toInt()
    private fun processAutoScroll(event: MotionEvent) {
        val y = event.y.toInt()
        if (y < autoScrollDistance) {
            lastX = event.x
            lastY = event.y
            scrollDistance = -(autoScrollDistance - y) / SCROLL_FACTOR
            if (!inTopSpot) {
                inTopSpot = true
                startAutoScroll()
            }
        } else if (y > height - autoScrollDistance) {
            lastX = event.x
            lastY = event.y
            scrollDistance = (y - height + autoScrollDistance) / SCROLL_FACTOR
            if (!inBottomSpot) {
                inBottomSpot = true
                startAutoScroll()
            }
        } else {
            inBottomSpot = false
            inTopSpot = false
            lastX = java.lang.Float.MIN_VALUE
            lastY = java.lang.Float.MIN_VALUE
            stopAutoScroll()
        }
    }

    fun getSelectRange(): Pair<Pair<Int, SelectItem>, Pair<Int, SelectItem>>? {
        var selectStart = selectStart ?: return null
        var selectEnd = selectEnd ?: return null
        var startIndex = bookAdapter.data.indexOf(selectStart.item)
        var endIndex = bookAdapter.data.indexOf(selectEnd.item)
        if (startIndex > endIndex || (startIndex == endIndex && selectStart.pos > selectEnd.pos)) {
            startIndex = endIndex.also { endIndex = startIndex }
            selectStart = selectEnd.also { selectEnd = selectStart }
        }
        return (startIndex to selectStart) to (endIndex to selectEnd)
    }

    private val actionModeCallback = object : SelectableActionMode() {
        val clipboardManager by lazy { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val (startPair, endPair) = getSelectRange() ?: return false
            val (startIndex, selectStart) = startPair
            val (endIndex, selectEnd) = endPair
            val str = (adapter as? TextSelectionAdapter)?.getSelectionText(
                startIndex,
                endIndex,
                selectStart.pos,
                selectEnd.pos
            ) ?: ""
            when (item.itemId) {
                ID_COPY -> {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("novel_content", str))
                    Toast.makeText(App.app.host, "已复制到剪贴板", Toast.LENGTH_LONG).show()
                }
                ID_SHARE -> AppUtil.shareString(context, str)
            }
            hideActionMode()
            return true
        }

        override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: Rect) {
            super.onGetContentRect(mode, view, outRect)
            val (startPair, endPair) = getSelectRange() ?: return
            val (startIndex, selectStart) = startPair
            val (endIndex, selectEnd) = endPair
            val firstVisibleSelectPos = Math.max(startIndex, bookLayoutManager.findFirstVisibleItemPosition())
            val lastVisibleSelectPos = Math.min(endIndex, bookLayoutManager.findLastVisibleItemPosition())
            var left = -1
            var right = -1
            val textHeight = textSelectionAdapter.getTextHeight()
            val top = when {
                startIndex < firstVisibleSelectPos -> 0
                startIndex > lastVisibleSelectPos -> this@SelectableRecyclerView.height
                else -> bookLayoutManager.findViewByPosition(startIndex)?.let {
                    val p = textSelectionAdapter.getHandlePosition(it, selectStart.pos)
                    left = p.x
                    p.y - textHeight
                } ?: 0
            }
            val bottom = when {
                endIndex < firstVisibleSelectPos -> 0
                endIndex > lastVisibleSelectPos -> this@SelectableRecyclerView.height
                else -> bookLayoutManager.findViewByPosition(endIndex)?.let {
                    val p = textSelectionAdapter.getHandlePosition(it, selectEnd.pos)
                    right = if (p.x < 0) this@SelectableRecyclerView.width else p.x
                    bookLayoutManager.getDecoratedTop(it).coerceAtLeast(p.y)
                } ?: 0
            }
            if (left > 0 && right > 0 && top + textHeight == bottom) {
                outRect.set(left, top, right, bottom)
            } else {
                outRect.set(0, top, this@SelectableRecyclerView.width, bottom)
            }
        }
    }

    fun showActionMode() {
        actionMode = startActionMode(actionModeCallback, ActionMode.TYPE_FLOATING)
    }

    fun hideActionMode() {
        actionMode?.finish()
        actionMode = null
    }

    init {
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == SCROLL_STATE_IDLE && selectStart != null) {
                    showActionMode()
                } else {
                    hideActionMode()
                }
                return super.onScrollStateChanged(recyclerView, newState)
            }
        })
    }

    companion object {
        private const val MAX_SCROLL_DISTANCE = 16

        //这个数越大，滚动的速度增加越慢
        private const val SCROLL_FACTOR = 6
    }

}