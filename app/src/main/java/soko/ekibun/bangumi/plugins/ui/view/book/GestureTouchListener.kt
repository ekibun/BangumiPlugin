package soko.ekibun.bangumi.plugins.ui.view.book

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.GestureDetectorCompat
import soko.ekibun.bangumi.plugins.App

class GestureTouchListener : View.OnTouchListener {
    val listeners = ArrayList<GestureCallback>()

    private val scaleGestureDetector =
        ScaleGestureDetector(App.app.host, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
                listeners.forEach { it.onScaleBegin(detector) }
                return super.onScaleBegin(detector)
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                listeners.forEach { it.onScale(detector) }
                return super.onScale(detector)
            }
        })
    private val gestureDetector =
        GestureDetectorCompat(App.app.host, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                listeners.forEach { it.onSingleTapConfirmed(e) }
                return super.onSingleTapConfirmed(e)
            }

            override fun onLongPress(e: MotionEvent) {
                listeners.forEach { it.onLongPress(e) }
                super.onLongPress(e)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                listeners.forEach { it.onDoubleTap(e) }
                return super.onDoubleTap(e)
            }
        })

    abstract class GestureCallback {
        open fun onScaleBegin(detector: ScaleGestureDetector?) {}
        open fun onScale(detector: ScaleGestureDetector) {}
        open fun onSingleTapConfirmed(e: MotionEvent) {}
        open fun onLongPress(e: MotionEvent) {}
        open fun onDoubleTap(e: MotionEvent) {}
        open fun onTouch(e: MotionEvent) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        listeners.forEach { it.onTouch(event) }
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return false
    }
}