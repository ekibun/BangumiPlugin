package soko.ekibun.bangumi.plugins.ui.view

import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import kotlinx.android.synthetic.main.controller_extra.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.ui.view.controller.Controller
import soko.ekibun.bangumi.plugins.ui.view.controller.LargeController
import soko.ekibun.bangumi.plugins.ui.view.controller.SmallController
import java.util.*

class VideoController(view: ViewGroup,
                      private val actionListener: OnActionListener,
                      private val isFullScreen: ()->Boolean = {false}){
    interface OnActionListener {
        fun onPlayPause()
        fun onFullscreen()
        fun onNext()
        fun onPrev()
        fun onDanmaku()
        fun seekTo(pos: Long)
        fun onDanmakuSetting()
        fun onTitle()
        fun onShowHide(show: Boolean)
    }

    private val ctrExtra: View by lazy {
        (view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                .inflate(R.layout.controller_extra, view, true)
    }

    init{
        var downX = 0f
        var downY = 0f
        var downPos = 0L
        var lastTime = 0L
        var dblclick = false
        view.setOnTouchListener { _, event ->
            when(event.action){
                MotionEvent.ACTION_DOWN->{
                    val curTime = System.currentTimeMillis()
                    if(curTime - lastTime < 300){
                        dblclick = true
                        actionListener.onPlayPause()
                    }else{
                        dblclick = false
                    }
                    lastTime = curTime
                    downX = event.x
                    downY = event.y
                    downPos = position.toLong() * 10
                    view.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE->{
                    if(Math.abs(event.x -downX) > Math.abs(event.y -downY) && Math.abs(event.x -downX) >80){
                        doSkip = true//ctrlView.ctr_txt.visibility = View.VISIBLE
                        doShowHide(true)
                        resetTimeout(false)
                    }
                    updateProgress(downPos + (event.x - downX).toLong() * 66, true)
                    //controller.setProcessSkip(null, Math.abs(event.x -downX).toInt() * 66 / 10)
                }
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP-> {
                    if (doSkip) {
                        if(Math.abs(event.x - downX) > Math.abs(event.y - downY))
                            actionListener.seekTo(downPos + (event.x - downX).toLong() * 66)
                        doSkip = false
                    } else
                        if (!dblclick && System.currentTimeMillis() - lastTime < 300)
                            Handler().postDelayed({
                                if (!dblclick) {
                                    doShowHide(!isShow)
                                }
                            }, 300)
                    view.parent.requestDisallowInterceptTouchEvent(false)
                    resetTimeout()
                }
            }
            true
        }
    }

    private val onClick = { action: Controller.Action ->
        resetTimeout()
        when(action){
            Controller.Action.PLAY_PAUSE -> actionListener.onPlayPause()
            Controller.Action.FULLSCREEN -> actionListener.onFullscreen()
            Controller.Action.PREV -> actionListener.onPrev()
            Controller.Action.NEXT -> actionListener.onNext()
            Controller.Action.DANMAKU -> actionListener.onDanmaku()
            Controller.Action.TITLE -> actionListener.onTitle()
            Controller.Action.DANMAKU_SETTING -> actionListener.onDanmakuSetting()
        }
    }
    private var doSkip = false
    private val onSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(bar: SeekBar) {
            doSkip = true
            resetTimeout(false)
        }
        override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser) return
            updateProgress((progress / 10).toLong())
        }
        override fun onStopTrackingTouch(bar: SeekBar) {
            doSkip = false
            actionListener.seekTo(bar.progress.toLong() * 10)
            resetTimeout()
        }
    }
    private val controller = hashMapOf(
            ctrSmall to SmallController(view, onClick, onSeekBarChangeListener),
            ctrLarge to LargeController(view, onClick, onSeekBarChangeListener)
    )

    val isShow get() = controller[if(isFullScreen()) ctrLarge else ctrSmall]?.ctrLayout?.visibility == View.VISIBLE
    fun doShowHide(show: Boolean) {
        val ctr = controller[if(isFullScreen()) ctrLarge else ctrSmall]
        for(i in controller.values)
            i.doShowHide(false)
        ctr?.doShowHide(show || !ctrVisibility)
        if(show || ctrVisibility) actionListener.onShowHide(show)
        if(show){ resetTimeout() }else{ timeoutTask?.cancel() }
    }

    var ctrVisibility: Boolean = true
        set(v) {
            for(i in controller.values)
                i.setCtrVisibility(v)
            field = v
        }

    //timer
    val timer = Timer()
    private var timeoutTask: TimerTask? = null
    private fun resetTimeout(timeout: Boolean = true){
        //finish timeout task
        timeoutTask?.cancel()
        if(timeout){
            //doAdd timeout task
            timeoutTask = object: TimerTask(){
                override fun run() {
                    doShowHide(false)
                }
            }
            timer.schedule(timeoutTask, 3000)
        }
    }

    var duration = 0
    var buffedPosition = 0
    private var position = 0
    fun updateProgress(posLong: Long, skip: Boolean = false){
        if(!doSkip || skip){
            position = Math.max(0, (posLong / 10).toInt())
            for(i in controller.values)
                i.updateProgress(position, duration, buffedPosition)
        }
    }

    fun updatePauseResume(isPlaying: Boolean) {
        for(i in controller.values)
            i.updatePauseResume(isPlaying)
    }

    fun updateDanmaku(show: Boolean){
        for(i in controller.values)
            i.updateDanmaku(show)
    }

    fun updatePrevNext(hasPrev: Boolean, hasNext: Boolean){
        for(i in controller.values)
            i.updatePrevNext(hasPrev, hasNext)
    }

    fun setTitle(title: String){
        for(i in controller.values)
            i.setTitle(title)
    }

    fun updateLoading(show: Boolean){
        ctrExtra.post {
            ctrExtra.ctr_load.visibility = if(show) View.VISIBLE else View.INVISIBLE
        }
    }

    companion object {
        private const val ctrSmall = 0
        private const val ctrLarge = 1
    }
}