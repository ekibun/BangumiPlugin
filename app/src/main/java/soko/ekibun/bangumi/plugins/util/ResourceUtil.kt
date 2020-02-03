package soko.ekibun.bangumi.plugins.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

object ResourceUtil {
    fun dip2px(context: Context, dpValue: Float): Int{
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun getDrawable(context: Context, @DrawableRes resId: Int): Drawable {
        return context.resources.getDrawable(resId, context.theme)
    }

    @ColorInt
    fun resolveColorAttr(context: Context, @AttrRes colorAttr: Int): Int {
        val resolvedAttr = resolveThemeAttr(context, colorAttr)
        val colorRes = if (resolvedAttr.resourceId != 0) resolvedAttr.resourceId else resolvedAttr.data
        return ContextCompat.getColor(context, colorRes)
    }

    private fun resolveThemeAttr(context: Context, @AttrRes attrRes: Int): TypedValue {
        val theme = context.theme
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue
    }

    fun getId(context: Context, param: String): Int {
        return getResId(context, "id", param)
    }

    fun getResId(context: Context, type: String, param: String): Int {
        return context.resources.getIdentifier(param, type, context.packageName)
    }
}