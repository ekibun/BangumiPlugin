package soko.ekibun.bangumi.plugins.ui.view.book

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

class LetterSpacingSpan(private val width: Int, private val spacing: Float) : ReplacementSpan() {
    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        return width
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        canvas.drawText(text ?: "", start, end, x + spacing, y.toFloat(), paint)
    }
}