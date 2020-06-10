package soko.ekibun.bangumi.plugins.provider.music

import android.view.View
import android.widget.SeekBar
import kotlinx.android.synthetic.main.plugin_music.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.ui.view.controller.Controller
import kotlin.jvm.internal.Intrinsics


class MusicController(view: View, onActionListener: OnActionListener) {
    var buffedPosition = 0
    private val ctrView: View

    var doSkip = false
    var duration = 0
    private var position = 0

    interface OnActionListener {
        fun onInfo()
        fun onNext()
        fun onPlayPause()
        fun onPrev()
        fun onRepeat()
        fun seekTo(pos: Long)
    }

    fun updateLoading(loading: Boolean) {}

    fun updateProgress(posLong: Long, skip: Boolean = false) {
        if (!doSkip || skip) {
            position = Math.max(0, (posLong / 10).toInt())
            updateProgress(position, duration, buffedPosition)
        }
    }

    fun updateProgress(pos: Int, dur: Int, buf: Int) {
        if (Thread.currentThread() != ctrView.context.mainLooper.thread) {
            ctrView.post { updateProgress(pos, dur, buf) }
            return
        }
        ctrView.ctr_seek.max = dur
        ctrView.ctr_seek.progress = pos
        ctrView.ctr_seek.secondaryProgress = buf
        ctrView.ctr_time.text = Controller.stringForTime(pos)
        ctrView.ctr_total_time.text = Controller.stringForTime(dur)
    }

    fun updatePauseResume(isPlaying: Boolean) {
        if (Thread.currentThread() != ctrView.context.mainLooper.thread) {
            ctrView.post { updatePauseResume(isPlaying) }
            return
        }
        ctrView.ctr_pause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    fun updatePrevNext(hasPrev: Boolean, hasNext: Boolean) {
        if (Thread.currentThread() != ctrView.context.mainLooper.thread) {
            ctrView.post { updatePrevNext(hasPrev, hasNext) }
            return
        }
        ctrView.ctr_prev?.alpha = if (hasPrev) 1f else 0.5f
        ctrView.ctr_next?.alpha = if (hasNext) 1f else 0.5f
    }

    init {
        Intrinsics.checkParameterIsNotNull(view, "ctrView")
        Intrinsics.checkParameterIsNotNull(onActionListener, "actionListener")
        ctrView = view
        ctrView.ctr_pause.setOnClickListener {
            onActionListener.onPlayPause()
        }
        ctrView.ctr_next.setOnClickListener {
            onActionListener.onNext()
        }
        ctrView.ctr_prev.setOnClickListener {
            onActionListener.onPrev()
        }
        ctrView.ctr_repeat.setOnClickListener {
            onActionListener.onRepeat()
        }
        ctrView.ctr_info.setOnClickListener {
            onActionListener.onInfo()
        }
        ctrView.ctr_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(bar: SeekBar) {
                doSkip = true
            }

            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                updateProgress((progress / 10).toLong())
            }

            override fun onStopTrackingTouch(bar: SeekBar) {
                doSkip = false
                onActionListener.seekTo(bar.progress.toLong() * 10)
            }
        })
    }
}