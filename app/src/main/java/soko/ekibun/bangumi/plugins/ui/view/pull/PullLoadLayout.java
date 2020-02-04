package soko.ekibun.bangumi.plugins.ui.view.pull;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.annotation.IntDef;
import com.tk.anythingpull.ILoad;
import com.tk.anythingpull.IRefresh;
import com.tk.anythingpull.Utils;
import soko.ekibun.bangumi.plugins.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * <pre>
 *     author : TK
 *     time   : 2017/04/11
 *     desc   : 万能容器，下拉刷新，上拉加载
 * </pre>
 */
public class PullLoadLayout extends ViewGroup {
    private static final String TAG = "AnythingPullLayout";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({INIT,
            PRE_REFRESH, TO_REFRESH, REFRESH_ING, REFRESH_RESULT,
            PRE_LOAD, TO_LOAD, LOAD_ING, LOAD_RESULT})
    public @interface Status {
    }

    public static final float swipeFactor = 2f;

    /**
     * 待机状态（无触摸）
     */
    public static final int INIT = 0;
    /**
     * 预刷新，下拉方向，释放不触发刷新
     */
    public static final int PRE_REFRESH = 1;
    /**
     * 释放将触发下拉刷新
     */
    public static final int TO_REFRESH = 2;
    /**
     * 下拉刷新ing
     */
    public static final int REFRESH_ING = 3;
    /**
     * 刷新结果停留
     */
    public static final int REFRESH_RESULT = 4;
    /**
     * 预加载，上拉方向，释放不触发加载
     */
    public static final int PRE_LOAD = 5;
    /**
     * 释放将触发上拉加载
     */
    public static final int TO_LOAD = 6;
    /**
     * 上拉加载ing
     */
    public static final int LOAD_ING = 7;
    /**
     * 加载结果停留
     */
    public static final int LOAD_RESULT = 8;
    /**
     * 普通拉动模式
     */
    public static final int MODE_PULL = 0;
    /**
     * 层级拉动模式
     */
    public static final int MODE_LAYER = 2;
    /**
     * 抽屉拉动模式
     */
    public static final int MODE_DST = 3;
    /**
     * 当前状态
     */
    private int mStatus;
    /**
     * 第一次加载视图
     */
    private boolean layoutInflate;
    /**
     * Adapter模式
     */
    private int refreshMode;
    private int loadMode;
    /**
     * 刷新、加载视图适配器
     */
    private Adapter refreshAdapter;
    private Adapter loadAdapter;
    /**
     * 上拉、下拉视图相关
     */
    private IRefresh iRefresh;
    private ILoad iLoad;
    private View refreshView;
    private View loadView;
    private int refreshViewHeight = 0;
    private int loadViewHeight = 0;
    /**
     * 主体视图
     */
    private View contentView;
    /**
     * 下拉刷新、上拉加载拖动距离
     */
    private int refreshDistance = 0;
    private int loadDistance = 0;
    /**
     * 上次触点位置，会随MOVE事件补正
     */
    private int lastY;
    /**
     * 阻力
     */
    private float refreshResistance;
    private float loadResistance;
    /**
     * 回弹动画时长
     */
    private int refreshCloseDuring;
    private int loadCloseDuring;
    /**
     * 结果停留时长
     */
    private int refreshResultDuring;
    private int loadResultDuring;
    /**
     * 刷新、加载视图固定
     */
    private boolean refreshFixed;
    private boolean loadFixed;
    /**
     * 是否开启下拉刷新、上拉加载功能
     */
    private boolean refreshEnable;
    private boolean loadEnable;
    /**
     * 回弹动画
     */
    private ValueAnimator animator;
    /**
     * 触摸阈值
     */
    private int touchSlop;
    /**
     * 滑动时取消事件
     */
    private boolean hasCancel;
    /**
     * 回调监听
     */
    private OnPullListener onPullListener;

    public PullLoadLayout(Context context) {
        this(context, null);
    }

    public PullLoadLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullLoadLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        @SuppressLint("CustomViewStyleable") TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.AnythingPullLayout);

        refreshMode = array.getInt(R.styleable.AnythingPullLayout_refresh_mode, MODE_PULL);
        refreshResistance = array.getFloat(R.styleable.AnythingPullLayout_refresh_resistance, 1.6F);
        refreshCloseDuring = array.getInt(R.styleable.AnythingPullLayout_refresh_close_during, 300);
        refreshResultDuring = array.getInt(R.styleable.AnythingPullLayout_refresh_result_during, 750);
        refreshFixed = array.getBoolean(R.styleable.AnythingPullLayout_refresh_fixed, false);
        refreshEnable = array.getBoolean(R.styleable.AnythingPullLayout_refresh_enable, true);

        loadMode = array.getInt(R.styleable.AnythingPullLayout_load_mode, MODE_PULL);
        loadResistance = array.getFloat(R.styleable.AnythingPullLayout_load_resistance, 1.6F);
        loadCloseDuring = array.getInt(R.styleable.AnythingPullLayout_load_close_during, 300);
        loadResultDuring = array.getInt(R.styleable.AnythingPullLayout_load_result_during, 750);
        loadFixed = array.getBoolean(R.styleable.AnythingPullLayout_load_fixed, false);
        loadEnable = array.getBoolean(R.styleable.AnythingPullLayout_load_enable, true);

        array.recycle();
        touchSlop = Utils.dp2px(1);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layoutInflate();
        layoutSelf(false, 0);
    }

    private void layoutSelf(final boolean touch, final int mode) {
        if (refreshDistance < 0) {
            refreshDistance = 0;
        }
        if (loadDistance < 0) {
            loadDistance = 0;
        }
        int resultRefreshDistance = refreshDistance - refreshAdapter.pullConsumed(refreshDistance);
        int resultLoasDistance = loadDistance - loadAdapter.pullConsumed(loadDistance);
        MarginLayoutParams lp = (MarginLayoutParams) contentView.getLayoutParams();
        int contentViewLeft = getPaddingLeft() + lp.leftMargin;
        int contentViewTop = getPaddingTop() + +lp.topMargin +
                (resultRefreshDistance - resultLoasDistance);
        int contentViewRight = contentViewLeft + contentView.getMeasuredWidth();
        int contentViewBottom = contentViewTop + contentView.getMeasuredHeight();

        contentView.layout(contentViewLeft, contentViewTop, contentViewRight, contentViewBottom);
        refreshAdapter.layout(refreshDistance, this);
        loadAdapter.layout(loadDistance, this);
        if (mode == -1) {
            //下拉刷新部分的位置监听回调
            if (mStatus == INIT) {
                //第一次执行
                mStatus = PRE_REFRESH;
                if (iRefresh != null) {
                    iRefresh.preShow();
                }
            }
            if (iRefresh != null) {
                if (refreshViewHeight > 0 && refreshDistance >= refreshViewHeight * swipeFactor) {
                    if (mStatus != TO_REFRESH && mStatus != REFRESH_ING && mStatus != REFRESH_RESULT) {
                        //释放立即刷新，如果再刷新中就不重复
                        mStatus = TO_REFRESH;
                    }
                } else if (mStatus != REFRESH_ING && mStatus != REFRESH_RESULT) {
                    mStatus = PRE_REFRESH;
                }
                iRefresh.onPositionChange(touch, refreshDistance, mStatus);
            }

        } else if (mode == 1) {
            //上拉加载部分的位置监听回调
            if (mStatus == INIT) {
                //第一次执行
                mStatus = PRE_LOAD;
                if (iLoad != null) {
                    iLoad.preShow();
                }
            }
            if (iLoad != null) {
                if (loadViewHeight > 0 && loadDistance >= loadViewHeight * swipeFactor) {
                    if (mStatus != TO_LOAD && mStatus != LOAD_ING && mStatus != LOAD_RESULT) {
                        //释放立即加载，如果再加载中就不重复
                        mStatus = TO_LOAD;
                    }
                } else if (mStatus != LOAD_ING && mStatus != LOAD_RESULT) {
                    mStatus = PRE_LOAD;
                }
                iLoad.onPositionChange(touch, loadDistance, mStatus);
            }
        }
    }

    /**
     * 处理位置变化
     *
     * @param from          距离
     * @param to            距离
     * @param during        时长
     * @param animDirection 动画方向 1 向上
     * @param mode          -1：下拉 1：上拉
     * @param toStatus      最终状态
     */
    int lastValue = 0;
    private void processPosition(int from, int to, int during,
                                 final int animDirection, final int mode, final @Status int toStatus) {
        final int aniTo = to;
        switch (mStatus) {
            case PRE_REFRESH:
                if (iRefresh != null) {
                    iRefresh.preDismiss();
                }
                break;
            case PRE_LOAD:
                if (iLoad != null) {
                    iLoad.preDismiss();
                }
                break;
            default:
                break;
        }

        animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(during);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        lastValue = from;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int i = (int) animation.getAnimatedValue();
                if (mode == -1) {
                    refreshDistance = i;
                } else if (mode == 1) {
                    loadDistance = i;
                }
                if(aniTo == 0){
                    contentView.scrollBy(0, mode * (lastValue - i));
                    lastValue = i;
                }
                layoutSelf(false, mode);
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                onAnimEnd(animDirection, mode, toStatus);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }

    private void onAnimEnd(final int animDirection, final int mode, final @Status int toStatus) {
        switch (toStatus) {
            case INIT:
                if (animDirection == 1) {
                    if (iRefresh != null && mode == -1) {
                        iRefresh.onDismiss();
                        iRefresh.onPositionChange(false, 0, toStatus);
                    }
                } else if (animDirection == -1) {
                    if (iLoad != null && mode == 1) {
                        iLoad.onDismiss();
                        iLoad.onPositionChange(false, 0, toStatus);
                    }
                }
                break;
            case REFRESH_ING:
                if (iRefresh != null && mStatus != REFRESH_ING && mStatus != REFRESH_RESULT) {
                    iRefresh.onRefreshStart();
                    iRefresh.onPositionChange(false, refreshViewHeight, toStatus);
                    if (onPullListener != null) {
                        onPullListener.onRefreshStart(this);
                    }
                }
                break;
            case LOAD_ING:
                if (iLoad != null && mStatus != LOAD_ING && mStatus != LOAD_RESULT) {
                    iLoad.onLoadStart();
                    iLoad.onPositionChange(false, loadViewHeight, toStatus);
                    if (onPullListener != null) {
                        onPullListener.onLoadStart(this);
                    }
                }
                break;
            default:
                break;
        }
        mStatus = toStatus;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int count = getChildCount();
        if (count > 3) {
            throw new IllegalStateException(TAG + " children count must <= 3 !");
        }
        if (count == 0) {
            throw new IllegalStateException(TAG + " no children !");
        }
    }

    private void layoutInflate() {
        if (layoutInflate) {
            return;
        }
        View child;
        for (int i = 0, count = getChildCount(); i < count; i++) {
            child = getChildAt(i);
            if (child instanceof IRefresh) {
                if (refreshMode != 1) {
                    iRefresh = (IRefresh) child;
                    refreshView = child;
                    refreshViewHeight = child.getMeasuredHeight();
                }
            } else if (child instanceof ILoad) {
                if (loadMode != 1) {
                    iLoad = (ILoad) child;
                    loadView = child;
                    loadViewHeight = child.getMeasuredHeight();
                }
            } else {
                contentView = child;
            }
        }
        initAdapterMode(refreshMode, loadMode);
        layoutInflate = true;
    }

    public void initAdapterMode(int refreshMode, int loadMode) {
        this.refreshMode = refreshMode;
        this.loadMode = loadMode;
        if (loadView == null) {
            loadAdapter = empterLoadAdapter;
        } else {
            switch (loadMode) {
                case MODE_PULL:
                    loadAdapter = new LoadPullAdapter(loadView);
                    break;
                default:
                    loadAdapter = empterLoadAdapter;
                    break;
            }
        }
        if (refreshView == null) {
            refreshAdapter = empterRefreshAdapter;
        } else {
            switch (refreshMode) {
                case MODE_PULL:
                    refreshAdapter = new RefreshPullAdapter(refreshView);
                    break;
                default:
                    refreshAdapter = empterRefreshAdapter;
                    break;
            }
        }
        initLayer();
    }

    private void initLayer() {
        int refreshLayer = refreshAdapter.getLayer();
        int loadLayer = loadAdapter.getLayer();
        if (loadLayer > 0) {
            if (refreshLayer > 0) {
                loadFront();
                refreshFront();
            } else {
                contentFront();
                loadFront();
            }
        } else if (loadLayer == 0) {
            if (refreshLayer < 0) {
                loadFront();
                contentFront();
            } else {
                refreshFront();
            }
        } else {
            if (refreshLayer < 0) {
                refreshFront();
                contentFront();
            } else {
                contentFront();
                refreshFront();
            }
        }
    }

    private void refreshFront() {
        if (refreshView != null) {
            refreshView.bringToFront();
        }
    }

    private void contentFront() {
        if (contentView != null) {
            contentView.bringToFront();
        }
    }

    private void loadFront() {
        if (loadView != null) {
            loadView.bringToFront();
        }
    }

    /**
     * 重写分发规则，防止子控件requestDisallowInterceptTouchEvent
     *
     * @param event
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return super.dispatchTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //取消之前的动画
                if (animator != null && animator.isRunning()) {
                    animator.cancel();
                }
                hasCancel = false;
                lastY = (int) event.getY();
                super.dispatchTouchEvent(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                //下拉上拉不允许同时存在

                int dy = (int) (lastY - event.getY());
                if (event.getY() > lastY) {
                    //下拉
                    if (loadDistance > 0) {
                        //上拉过程中下拉，默认无阻力
                        if (loadFixed && (mStatus == LOAD_ING || mStatus == LOAD_RESULT)) {
                            break;
                        }
                        lastY = (int) event.getY();
                        if (!checkLoad(dy)) {
                            return true;
                        }
                        cancelEvent(dy, event);

                        loadDistance += dy;
                        layoutSelf(true, 1);
                        if (loadDistance == 0 && contentView.canScrollVertically(-1)) {
                            //恢复事件，可以滚动衔接
                            resetEvent(event);
                        }
                        return true;
                    }
                    if (!contentView.canScrollVertically(-1)) {
                        //内容区域下拉到顶了
                        if (!refreshEnable) {
                            break;
                        }
                        if (loadDistance > 0) {
                            //上拉加载部分尚在显示中
                            break;
                        }
                        //阻力
                        dy = (int) (dy / refreshResistance);
                        lastY = (int) event.getY();
                        if (!checkRefresh(dy)) {
                            return true;
                        }
                        cancelEvent(dy, event);

                        refreshDistance -= dy;
                        layoutSelf(true, -1);
                        return true;
                    }
                } else {
                    //上拉
                    if (refreshDistance > 0) {
                        //下拉状态中上拉，默认无阻力
                        if (refreshFixed && (mStatus == REFRESH_ING || mStatus == REFRESH_RESULT)) {
                            break;
                        }
                        //下拉状态中上拉，默认无阻力
                        lastY = (int) event.getY();
                        if (!checkRefresh(dy)) {
                            return true;
                        }
                        cancelEvent(dy, event);

                        refreshDistance -= dy;
                        layoutSelf(true, -1);
                        if (refreshDistance == 0 && contentView.canScrollVertically(1)) {
                            //恢复事件，可以滚动衔接
                            resetEvent(event);
                        }
                        return true;
                    }
                    if (!contentView.canScrollVertically(1)) {
                        //内容区域上拉到底了
                        if (!loadEnable) {
                            break;
                        }
                        if (refreshDistance > 0) {
                            //下拉刷新部分尚在显示中
                            break;
                        }
                        //阻力
                        dy = (int) (dy / loadResistance);
                        lastY = (int) event.getY();
                        if (!checkLoad(dy)) {
                            return true;
                        }
                        cancelEvent(dy, event);

                        loadDistance += dy;
                        layoutSelf(true, 1);
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mStatus != REFRESH_ING && refreshDistance > 0 && (refreshDistance < refreshViewHeight * swipeFactor || refreshViewHeight == 0)) {
                    //回弹<下拉刷新部分>
                    processPosition(refreshDistance, 0, refreshCloseDuring, 1, -1, INIT);
                    break;
                }
                if ((mStatus == REFRESH_ING && refreshDistance >= refreshViewHeight) || (refreshDistance >= refreshViewHeight * swipeFactor && refreshViewHeight > 0)) {
                    //回弹至触发下拉刷新
                    processPosition(refreshDistance, refreshViewHeight, refreshCloseDuring, 1, -1, REFRESH_ING);
                    break;
                }
                if (mStatus != LOAD_ING && loadDistance > 0 && (loadDistance < loadViewHeight * swipeFactor || loadViewHeight == 0)) {
                    //回弹<上拉加载部分>
                    processPosition(loadDistance, 0, loadCloseDuring, -1, 1, INIT);
                    break;
                }
                if ((mStatus == LOAD_ING && loadDistance >= loadViewHeight) || (loadDistance >= loadViewHeight * swipeFactor && loadViewHeight > 0)) {
                    //回弹至触发下拉刷新
                    processPosition(loadDistance, loadViewHeight, loadCloseDuring, -1, 1, LOAD_ING);
                    break;
                }
                break;
            default:
                break;
        }
        lastY = (int) event.getY();
        return super.dispatchTouchEvent(event);
    }

    /**
     * 取消事件
     *
     * @param dy
     * @param event
     */
    private void cancelEvent(int dy, MotionEvent event) {
        if (Math.abs(dy) > touchSlop && (!hasCancel)) {
            //cancel事件
            MotionEvent e = MotionEvent.obtain(event.getDownTime(), event.getEventTime() + ViewConfiguration.getLongPressTimeout(),
                    MotionEvent.ACTION_CANCEL, event.getX(), event.getY(), event.getMetaState());
            super.dispatchTouchEvent(e);
            hasCancel = true;
        }
    }

    /**
     * 恢复事件响应
     *
     * @param event
     */
    private void resetEvent(MotionEvent event) {
        MotionEvent e = MotionEvent.obtain(event.getDownTime(), event.getEventTime(),
                MotionEvent.ACTION_DOWN, event.getX(), event.getY(), event.getMetaState());
        super.dispatchTouchEvent(e);
    }

    /**
     * 刷新中的后续触摸判断
     *
     * @param dy
     * @return
     */
    private boolean checkRefresh(int dy) {
        if (Math.abs(dy) < touchSlop && (mStatus == REFRESH_ING || mStatus == REFRESH_RESULT)) {
            return false;
        }
        return true;
    }

    /**
     * 加载中的后续触摸判断
     *
     * @param dy
     * @return
     */
    private boolean checkLoad(int dy) {
        if (Math.abs(dy) < touchSlop && (mStatus == LOAD_ING || mStatus == LOAD_RESULT)) {
            return false;
        }
        return true;
    }

    /**
     * 自动下拉刷新
     */
    public void autoRefresh() {
        if (!refreshEnable || refreshMode == 1) {
            return;
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                processPosition(0, refreshViewHeight, refreshCloseDuring, -1, -1, REFRESH_ING);
            }
        }, 500);
    }

    /**
     * 自动上拉加载，注意load_mode
     */
    public void autoLoad() {
        if (!loadEnable || loadMode == 1) {
            return;
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                processPosition(0, loadViewHeight, loadCloseDuring, 1, 1, LOAD_ING);
            }
        }, 500);
    }

    /**
     * 下拉刷新回调
     *
     * @param success
     */
    public void responseRefresh(boolean success) {
        if (iRefresh != null && mStatus == REFRESH_ING) {
            iRefresh.onRefreshFinish(success);
            mStatus = REFRESH_RESULT;
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    iRefresh.preDismiss();
                    processPosition(refreshDistance, 0, refreshCloseDuring, 1, -1, INIT);
                }
            }, refreshResultDuring);
        }
    }

    /**
     * 上拉结果回调
     *
     * @param success
     */
    public void responseload(boolean success) {
        if (iLoad != null && mStatus == LOAD_ING) {
            iLoad.onLoadFinish(success);
            mStatus = LOAD_RESULT;
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    iLoad.preDismiss();
                    processPosition(loadDistance, 0, loadCloseDuring, -1, 1, INIT);
                }
            }, loadResultDuring);
        }
    }

    /**
     * 禁用下拉刷新功能，建议xml配置
     *
     * @param refreshEnable
     */
    @Deprecated
    public void setRefreshEnable(boolean refreshEnable) {
        this.refreshEnable = refreshEnable;
    }

    /**
     * 禁用上拉加载功能，建议xml配置
     *
     * @param loadEnable
     */
    @Deprecated
    public void setLoadEnable(boolean loadEnable) {
        this.loadEnable = loadEnable;
    }

    /**
     * 下拉刷新部分是否固定，建议xml配置
     *
     * @param refreshFixed
     */
    @Deprecated
    public void setRefreshFixed(boolean refreshFixed) {
        this.refreshFixed = refreshFixed;
    }

    /**
     * 上拉加载部分是否固定，建议xml配置
     *
     * @param loadFixed
     */
    @Deprecated
    public void setLoadFixed(boolean loadFixed) {
        this.loadFixed = loadFixed;
    }

    public void setOnPullListener(OnPullListener onPullListener) {
        this.onPullListener = onPullListener;
    }

    public interface OnPullListener {
        void onRefreshStart(final PullLoadLayout pullLayout);

        void onLoadStart(final PullLoadLayout pullLayout);
    }

    private static Adapter empterRefreshAdapter = new ViewAdapter(null) {
        @Override
        public void layout(int distance, PullLoadLayout pullLayout) {

        }
    };
    private static Adapter empterLoadAdapter = new ViewAdapter(null) {
        @Override
        public void layout(int distance, PullLoadLayout pullLayout) {

        }
    };
}