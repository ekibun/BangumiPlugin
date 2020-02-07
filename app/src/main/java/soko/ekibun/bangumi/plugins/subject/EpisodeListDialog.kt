package soko.ekibun.bangumi.plugins.subject

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.dialog_episode_list.view.*
import kotlinx.android.synthetic.main.item_episode.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.VideoCache
import soko.ekibun.bangumi.plugins.provider.video.VideoPluginView
import soko.ekibun.bangumi.plugins.service.DownloadService
import soko.ekibun.bangumi.plugins.ui.view.BasePluginDialog
import java.io.IOException

/**
 * 剧集列表对话框
 */
class EpisodeListDialog(private val linePresenter: LinePresenter, val adapter: EpisodeAdapter) : BasePluginDialog(linePresenter.activity, linePresenter.pluginContext, R.layout.dialog_episode_list) {
    companion object {
        /**
         * 显示对话框
         */
        fun showDialog(linePresenter: LinePresenter) {
            val dialog = EpisodeListDialog(linePresenter, linePresenter.subjectView.episodeDetailAdapter)
            dialog.show()
        }
    }

    override val title: String = ""

    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onViewCreated(view: View) {
        val behavior = BottomSheetBehavior.from(view.bottom_sheet)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            @SuppressLint("SwitchIntDef")
            override fun onStateChanged(bottomSheet: View, @BottomSheetBehavior.State newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) dismiss()
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) { /* no-op */
            }
        })

        behavior.isHideable = true
        view.post {
            behavior.peekHeight = view.height * 2 / 3
            view.bottom_sheet_container.invalidate()
        }

        adapter.setUpWithRecyclerView(view.bottom_sheet_container)
        view.bottom_sheet_container.layoutManager = LinearLayoutManager(linePresenter.pluginContext)

        adapter.setOnItemChildLongClickListener { _, _, position ->
            val item = linePresenter.subjectView.episodeDetailAdapter.data[position]
            val videoCache = linePresenter.app.videoCacheModel.getVideoCache(item.t, linePresenter.subject)
            if(videoCache != null) DownloadService.remove(linePresenter.pluginContext, item.t, linePresenter.subject)
            true
        }

        adapter.setOnItemChildClickListener { _, _, position ->
            val item = linePresenter.subjectView.episodeDetailAdapter.data[position]
            val info = linePresenter.app.lineInfoModel.getInfos(linePresenter.subject)?.getDefaultProvider()?:return@setOnItemChildClickListener
            linePresenter.subjectView.episodeDetailAdapter.getViewByPosition(position, R.id.item_layout)?.let{
                it.item_download_info.text = "获取视频信息"
            }
            val videoModel = (linePresenter.pluginView as? VideoPluginView)?.videoModel
            videoModel?.getVideo(item.t.parseSort(linePresenter.pluginContext), item.t, info, {videoInfo, error->
                adapter.getViewByPosition(position, R.id.item_layout)?.let{
                    it.item_download_info.text = if(videoInfo != null)"解析视频地址" else "获取视频信息出错：${error?.message}"
                }
            }, {request, _, error ->
                adapter.getViewByPosition(position, R.id.item_layout)?.let{
                    it.item_download_info.text = "解析视频地址出错：${error?.message}"
                    if(request == null || request.url.startsWith("/")) return@let
                    it.item_download_info.text = "创建视频请求"
                    videoModel.createDownloadRequest(request, object: DownloadHelper.Callback{
                        override fun onPrepared(helper: DownloadHelper) {
                            val downloadRequest = helper.getDownloadRequest(request.url, null)
                            Log.v("downloadRequest", downloadRequest.streamKeys.toString())
                            DownloadService.download(linePresenter.pluginContext, item.t, linePresenter.subject, VideoCache(item.t, downloadRequest.type, downloadRequest.streamKeys, request))
                        }
                        override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                            it.item_download_info.post { it.item_download_info.text = e.toString() }
                        }
                    })
                }
            }, {
                AlertDialog.Builder(linePresenter.pluginContext).setMessage("正在使用非wifi网络").setPositiveButton("继续缓存"){ _, _ -> it() }
                    .setNegativeButton("取消"){_, _ -> adapter.notifyItemChanged(position) }
                    .setOnDismissListener { adapter.notifyItemChanged(position) }.show()
            })
        }

        val paddingTop = view.bottom_sheet.paddingTop
        val paddingBottom = view.bottom_sheet_container.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.bottom_sheet.setPadding(
                view.bottom_sheet.paddingLeft,
                paddingTop + insets.systemWindowInsetTop,
                view.bottom_sheet.paddingRight,
                view.bottom_sheet.paddingBottom
            )
            view.bottom_sheet_container.setPadding(
                view.bottom_sheet_container.paddingLeft,
                view.bottom_sheet_container.paddingTop,
                view.bottom_sheet_container.paddingRight,
                paddingBottom + insets.systemWindowInsetBottom
            )
            insets
        }
    }
}
