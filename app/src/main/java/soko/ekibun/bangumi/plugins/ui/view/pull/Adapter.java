package soko.ekibun.bangumi.plugins.ui.view.pull;

/**
 * <pre>
 *     author : TK
 *     time   : 2017/04/11
 *     desc   : 适配器，借鉴Nested的思想
 * </pre>
 */
public abstract class Adapter {
    /**
     * @param dy >0 上拉手势
     * @return 需要消耗的距离，返回dy的值就是全额消耗，内容视图不发生位移
     */
    public int pullConsumed(int dy) {
        return 0;
    }

    /**
     * 扩展view layout
     *
     * @param distance   >=0
     * @param pullLayout
     */
    public abstract void layout(int distance, PullLoadLayout pullLayout);

    /**
     * 布局层次优先级，值越大优先级越高，同级时下拉刷新比上拉加载高
     *
     * @return
     */
    public abstract int getLayer();

}