package soko.ekibun.bangumi.plugins.ui.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class SelectableTextView constructor(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {
    override fun setText(text: CharSequence?, type: BufferType?) {
        setPadding(0, 0, 0, lineHeight - paint.getFontMetricsInt(null))
        super.setText(text, type)
    }
}