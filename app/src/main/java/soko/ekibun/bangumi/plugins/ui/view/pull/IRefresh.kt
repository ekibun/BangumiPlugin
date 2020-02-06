package soko.ekibun.bangumi.plugins.ui.view.pull

/**
 * <pre>
 * author : TK
 * time   : 2017/04/11
 * desc   : 下拉刷新视图，实现此接口
</pre> *
 */
interface IRefresh : IAction {
    /**
     * 开始刷新
     */
    fun onRefreshStart()

    /**
     * 结果回调
     *
     * @param success
     */
    fun onRefreshFinish(success: Boolean)
}