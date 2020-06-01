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
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.provider.Provider

/**
 * 防止Glide崩溃
 */
object GlideUtil {
    /**
     * Glide进度
     */
    fun loadWithProgress(
        req: Provider.HttpRequest,
        context: Context,
        view: ImageView,
        onProgress: (Float) -> Unit,
        callback: (Drawable) -> Unit,
        onError: (GlideException) -> Unit
    ): Target<Drawable>? {
        val request = with(context) ?: return null
        val header = req.header ?: HashMap()
        header["User-Agent"] = header["User-Agent"] ?: App.ua
        ProgressAppGlideModule.expect(req.url, object : ProgressAppGlideModule.UIonProgressListener {
            override fun onProgress(bytesRead: Long, expectedLength: Long) {
                onProgress(bytesRead * 1f / expectedLength)
            }

            override fun getGranualityPercentage(): Float {
                return 1.0f
            }
        })
        return request.asDrawable()
            .load(GlideUrl(req.url) {
                when {
                    !header.containsKey("referer") -> header.plus("referer" to req.url.toHttpUrlOrNull().toString())
                    header["referer"].isNullOrEmpty() -> header.minus("referer")
                    else -> header
                }
            })
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    e?.let { onError(it) }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

            })
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .into(object : ImageViewTarget<Drawable>(view) {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    callback(resource)
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
        } catch (e: IllegalStateException) {
            null
        }
    }

    fun with(activity: Activity): RequestManager? {
        return try {
            Glide.with(activity)
        } catch (e: IllegalStateException) {
            null
        }
    }

    fun with(activity: FragmentActivity): RequestManager? {
        return try {
            Glide.with(activity)
        } catch (e: IllegalStateException) {
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