package soko.ekibun.bangumi.plugins.subject

import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import soko.ekibun.bangumi.plugins.bean.Subject

interface ISubjectActivity {
    val subjectPresenter: ISubjectPresenter

    var onStopListener: () -> Unit
    var onPauseListener: () -> Unit
    var onResumeListener: () -> Unit
    var onDestroyListener: () -> Unit
    var onActivityResultListener: (Int, Int, Intent?) -> Unit
    var onUserLeaveHintListener: () -> Unit
    var onBackListener: () -> Boolean

    val item_plugin: FrameLayout
    val item_mask: View
    val app_bar: View

    interface ISubjectPresenter {
        val subjectView: ISubjectView

        val subject: Subject

        fun showEpisodeDialog(id: Int)
        fun updateSubjectProgress(vol: Int?, ep: Int?)

        var subjectRefreshListener: (Any?) -> Unit

        interface ISubjectView {
            val behavior: IBottomSheetBehavior
            val collapsibleAppBarHelper: ICollapsibleAppBarHelper

            var onStateChangedListener: (Int) -> Unit

            var detail: View

            var peakRatio: Float
            var peakMargin: Float

            interface IBottomSheetBehavior {
                var isHideable: Boolean

                @BottomSheetBehavior.State
                var state: Int
            }

            interface ICollapsibleAppBarHelper {
                var onTitleClickListener: (Any) -> Unit
            }
        }
    }
}