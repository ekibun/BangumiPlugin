package soko.ekibun.bangumi.plugins.ui.view.pull

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.tk.anythingpull.AnythingPullLayout
import com.tk.anythingpull.IAction
import com.tk.anythingpull.ILoad
import com.tk.anythingpull.IRefresh
import kotlinx.android.synthetic.main.item_pull_load.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.util.ResourceUtil

abstract class PullLoadView constructor(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs), IAction {
    abstract val hint: String
    private val progressDrawable by lazy {
        val dp = ResourceUtil.dip2px(context, 100f) / 100f
        val drawable = CircularProgressDrawable(context)
        drawable.setArrowDimensions(5 * dp,5 * dp)
        drawable.setColorSchemeColors(ResourceUtil.resolveColorAttr(context, R.attr.colorAccent))
        drawable.strokeWidth = 2 * dp
        drawable.centerRadius = 5 * dp
        drawable
    }

    val view by lazy {
        val view = LayoutInflater.from(context).inflate(R.layout.item_pull_load, this)
        view.item_progress.setImageDrawable(progressDrawable)
        view
    }

    @SuppressLint("SwitchIntDef")
    override fun onPositionChange(touch: Boolean, distance: Int, status: Int) {
        progressDrawable.arrowEnabled = status !in arrayOf(AnythingPullLayout.REFRESH_ING, AnythingPullLayout.LOAD_ING)
        if (progressDrawable.arrowEnabled){
            progressDrawable.alpha = Math.min(255, (distance * 255 / (1f + measuredHeight * 2f)).toInt())
            progressDrawable.setStartEndTrim(0f, Math.min(0.75f, distance / (1f + measuredHeight * 3f)))
            progressDrawable.progressRotation = distance * 0.01f
        }else{
            progressDrawable.alpha = 255
        }
        when(status){
            AnythingPullLayout.TO_REFRESH, AnythingPullLayout.TO_LOAD -> view.item_hint.text = "释放加载"
            AnythingPullLayout.REFRESH_ING, AnythingPullLayout.LOAD_ING -> view.item_hint.text = "加载中..."
            AnythingPullLayout.PRE_REFRESH, AnythingPullLayout.PRE_LOAD -> view.item_hint.text = hint
        }
        if (status == AnythingPullLayout.TO_REFRESH) view.item_hint.text = "释放加载"
    }

    fun onPullLoadFinish(success: Boolean) {
        progressDrawable.stop()
        view.item_progress.visibility = View.INVISIBLE
        view.item_hint.visibility = View.VISIBLE
        view.item_hint.text = if (success) "加载成功" else "加载失败"
    }

    fun onPullLoadStart() {
        progressDrawable.start()
        view.item_progress.visibility = View.VISIBLE
        view.item_hint.visibility = View.VISIBLE
        view.item_hint.text = "加载中..."
    }

    override fun preShow() {
        view.item_hint.text = hint
        view.item_progress.visibility = View.VISIBLE
        view.item_hint.visibility = View.VISIBLE
    }

    override fun preDismiss() {
        // preShow()
    }

    override fun onDismiss() {
        // preShow()
    }

    class PullLoadViewPrev(context: Context, attrs: AttributeSet) : PullLoadView(context, attrs), IRefresh {
        override val hint = "加载上一章"

        override fun onRefreshFinish(success: Boolean) {
            onPullLoadFinish(success)
        }

        override fun onRefreshStart() {
            onPullLoadStart()
        }
    }

    class PullLoadViewNext(context: Context, attrs: AttributeSet) : PullLoadView(context, attrs), ILoad {
        override val hint = "加载下一章"
        override fun onLoadStart() {
            onPullLoadStart()
        }

        override fun onLoadFinish(success: Boolean) {
            onPullLoadFinish(success)
        }
    }
}