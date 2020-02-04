@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.plugins.util

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition

/**
 * 防止Glide崩溃
 */
object GlideUtil {

    const val TYPE_RESOURCE = 0
    const val TYPE_PLACEHOLDER = 1
    const val TYPE_ERROR = 2

    /**
     * Glide进度
     */
    fun loadWithProgress(req: HttpUtil.HttpRequest, context: Context, view: ImageView, onProgress: (Float)->Unit, callback: (Int, Drawable?) -> Unit): Target<Drawable>? {
        val request = with(context) ?: return null
        ProgressAppGlideModule.expect(req.url, object : ProgressAppGlideModule.UIonProgressListener {
            override fun onProgress(bytesRead: Long, expectedLength: Long) {
                onProgress(bytesRead * 1f / expectedLength)
            }

            override fun getGranualityPercentage(): Float {
                return 1.0f
            }
        })
        return request.asDrawable().load(GlideUrl(req.url) {  if(!req.header.containsKey("referer")) req.header.plus("referer" to req.url) else req.header }).into(object : ImageViewTarget<Drawable>(view) {
            override fun onLoadStarted(placeholder: Drawable?) {
                super.onLoadStarted(placeholder)
                callback(TYPE_PLACEHOLDER, placeholder)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                callback(TYPE_ERROR, errorDrawable)
                super.onLoadFailed(errorDrawable)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                callback(TYPE_PLACEHOLDER, placeholder)
                super.onLoadCleared(placeholder)
            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                callback(TYPE_RESOURCE, resource)
                super.onResourceReady(resource, transition)
            }

            override fun onDestroy() {
                ProgressAppGlideModule.forget(req.url)
            }

            override fun setResource(resource: Drawable?) {
                view.setImageDrawable(resource)
            }
        })

    }

    fun with(context: Context): RequestManager? {
        return try {
            Glide.with(context)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun with(activity: Activity): RequestManager? {
        return try {
            Glide.with(activity)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun with(activity: FragmentActivity): RequestManager? {
        return try {
            Glide.with(activity)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun with(fragment: Fragment): RequestManager? {
        return try {
            Glide.with(fragment)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun with(view: View): RequestManager? {
        return try {
            Glide.with(view)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}