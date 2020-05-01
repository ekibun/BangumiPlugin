package soko.ekibun.bangumi.plugins.util

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import androidx.annotation.Keep
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.brotli.BrotliInterceptor
import okhttp3.internal.http.BridgeInterceptor
import okhttp3.logging.HttpLoggingInterceptor
import soko.ekibun.bangumi.plugins.App
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.zip.Inflater

object HttpUtil {
    val ua = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Mobile Safari/537.36"
    val httpCookieClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(30L, TimeUnit.SECONDS)
            .addNetworkInterceptor(HttpLoggingInterceptor().apply { this.level = HttpLoggingInterceptor.Level.BASIC })
            .addInterceptor(BrotliInterceptor).also {
                if (App.inited) it.addInterceptor(BridgeInterceptor(WebViewCookieHandler()))
            }.build()
    }

    data class HttpRequest(
        val url: String,
        val header: HashMap<String, String>? = null,
        val overrideExtension: String? = null
    )

    @Keep
    fun makeRequest(request: WebResourceRequest): HttpRequest {
        val headers = HashMap(request.requestHeaders)
        headers["cookie"] = CookieManager.getInstance().getCookie(request.url.host)?:""
        return HttpRequest(request.url.toString(), headers)
    }

    @Keep
    fun createBody(data: Map<String, String>): RequestBody{
        val builder = FormBody.Builder()
        data.forEach { builder.add(it.key, it.value) }
        return builder.build()
    }
    @Keep
    fun createBody(mediaType: String, data: String): RequestBody{
        return data.toRequestBody(mediaType.toMediaTypeOrNull())
    }
    @Keep
    fun getCall(url: String, header: Map<String, String> = HashMap(), body: RequestBody? = null): Call {
        val mutableHeader = header.toMutableMap()
        mutableHeader["User-Agent"] = header["User-Agent"] ?: ua
        val request = Request.Builder()
            .url(url)
            .headers(mutableHeader.toHeaders())
        if (body != null) request.post(body)
        return httpCookieClient.newCall(request.build())
    }

    @Keep
    fun getUrl(url: String, baseUri: URI?): String{
        return try{
            baseUri?.resolve(url)?.toASCIIString() ?: URI.create(url).toASCIIString()
        }catch (e: Exception){ url }
    }

    @Keep
    fun inflate(data: ByteArray, nowrap: Boolean = false): ByteArray {
        var output: ByteArray

        val inflater = Inflater(nowrap)
        inflater.reset()
        inflater.setInput(data)

        val o = ByteArrayOutputStream(data.size)
        try {
            val buf = ByteArray(1024)
            while (!inflater.finished()) {
                val i = inflater.inflate(buf)
                o.write(buf, 0, i)
            }
            output = o.toByteArray()
        } catch (e: java.lang.Exception) {
            output = data
            e.printStackTrace()
        } finally {
            try {
                o.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        inflater.end()
        return output
    }
}