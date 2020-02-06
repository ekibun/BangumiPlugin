package soko.ekibun.bangumi.plugins.ui.view.pull;

/**
 * <pre>
 *     author : TK
 *     time   : 2017/04/11
 *     desc   : 上拉加载视图，实现此接口
 * </pre>
 */
public interface ILoad extends IAction {
    /**
     * 开始加载
     */
    void onLoadStart();

    /**
     * 结果回调
     *
     * @param success
     */
    void onLoadFinish(boolean success);
}
