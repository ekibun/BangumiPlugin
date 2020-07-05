package soko.ekibun.bangumi.plugins.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.*
import java.io.IOException
import java.io.InputStream
import java.util.*

@GlideModule
class ProgressAppGlideModule : AppGlideModule() {
    // Disable manifest parsing to avoid adding similar modules twice.
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setLogLevel(Log.ERROR)
    }

    override fun registerComponents(
        context: Context,
        glide: Glide,
        registry: Registry
    ) {
        val client = OkHttpClient.Builder()
            .addNetworkInterceptor(object : Interceptor {
                @Throws(IOException::class)
                override fun intercept(chain: Interceptor.Chain): Response {
                    val request = chain.request()
                    Log.v("glide", request.toString())
                    val response = chain.proceed(request)
                    val listener: ResponseProgressListener = DispatchingProgressListener()
                    return response.newBuilder()
                        .body(OkHttpProgressResponseBody(request.url, response.body, listener))
                        .build()
                }
            })
            .build()
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(client)
        )
    }

    private interface ResponseProgressListener {
        fun update(url: HttpUrl, bytesRead: Long, contentLength: Long)
    }

    interface UIonProgressListener {
        fun onProgress(bytesRead: Long, expectedLength: Long)

        /**
         * Control how often the listener needs an update. 0% and 100% will always be dispatched.
         * @return in percentage (0.2 = call [.onProgress] around every 0.2 percent of progress)
         */
        val granualityPercentage: Float
    }

    private class DispatchingProgressListener internal constructor() : ResponseProgressListener {
        private val handler: Handler = Handler(Looper.getMainLooper())
        override fun update(url: HttpUrl, bytesRead: Long, contentLength: Long) {
            //System.out.printf("%s: %d/%d = %.2f%%%n", url, bytesRead, contentLength, (100f * bytesRead) / contentLength);
            var overrideContentLength = contentLength
            val key = url.toString()
            if (contentLength < 0 && bytesRead > 0) overrideContentLength = (1e9 / bytesRead).toLong() + bytesRead
            val listener = LISTENERS[key] ?: return
            if (overrideContentLength <= bytesRead) {
                forget(key)
            }
            if (needsDispatch(key, bytesRead, contentLength, listener.granualityPercentage)) {
                handler.post { listener.onProgress(bytesRead, overrideContentLength) }
            }
        }

        private fun needsDispatch(
            key: String,
            current: Long,
            total: Long,
            granularity: Float
        ): Boolean {
            if (granularity == 0f || current == 0L || total == current) {
                return true
            }
            val percent = 100f * current / total
            val currentProgress = (percent / granularity).toLong()
            val lastProgress = PROGRESSES[key]
            return if (lastProgress == null || currentProgress != lastProgress) {
                PROGRESSES[key] = currentProgress
                true
            } else {
                false
            }
        }

        companion object {
            private val LISTENERS: MutableMap<String, UIonProgressListener> =
                HashMap()
            private val PROGRESSES: MutableMap<String, Long> =
                HashMap()

            fun forget(url: String) {
                val httpUrl = url.toHttpUrlOrNull()?.toString() ?: url
                LISTENERS.remove(httpUrl)
                PROGRESSES.remove(httpUrl)
            }

            fun expect(url: String, listener: UIonProgressListener) {
                val httpUrl = url.toHttpUrlOrNull()?.toString() ?: url
                LISTENERS[httpUrl] = listener
            }
        }

    }

    private class OkHttpProgressResponseBody internal constructor(
        private val url: HttpUrl, private val responseBody: ResponseBody?,
        private val progressListener: ResponseProgressListener
    ) : ResponseBody() {
        private var bufferedSource: BufferedSource? = null
        override fun contentType(): MediaType? {
            return responseBody!!.contentType()
        }

        override fun contentLength(): Long {
            return responseBody!!.contentLength()
        }

        override fun source(): BufferedSource {
            if (bufferedSource == null) {
                bufferedSource = source(responseBody!!.source()).buffer()
            }
            return bufferedSource!!
        }

        private fun source(source: Source): Source {
            return object : ForwardingSource(source) {
                var totalBytesRead = 0L

                @Throws(IOException::class)
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    val fullLength = responseBody!!.contentLength()
                    if (bytesRead == -1L) { // this source is exhausted
                        totalBytesRead = fullLength
                    } else {
                        totalBytesRead += bytesRead
                    }
                    progressListener.update(url, totalBytesRead, fullLength)
                    return bytesRead
                }
            }
        }

    }

    companion object {
        fun forget(url: String) {
            DispatchingProgressListener.forget(url)
        }

        fun expect(url: String, listener: UIonProgressListener) {
            DispatchingProgressListener.expect(url, listener)
        }
    }
}