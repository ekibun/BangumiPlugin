package soko.ekibun.bangumi.plugins.bean

import android.net.Uri
import android.text.format.Formatter
import android.util.Log
import com.bumptech.glide.load.model.GlideUrl
import com.google.android.exoplayer2.offline.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.model.VideoModel
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.manga.MangaProvider
import soko.ekibun.bangumi.plugins.util.GlideUtil
import soko.ekibun.bangumi.plugins.util.HttpUtil
import soko.ekibun.bangumi.plugins.util.JsonUtil
import java.io.File

data class EpisodeCache(
    val episode: Episode,
    val type: String,
    var cache: String
) {
    fun cache(): Cache? = when (type) {
        Provider.TYPE_VIDEO -> JsonUtil.toEntity<VideoCache>(cache)
        Provider.TYPE_MANGA -> JsonUtil.toEntity<MangaCache>(cache)
        else -> null
    }

    fun remove() {
        cache()?.remove()
    }

    interface Cache {
        fun isFinished(): Boolean
        fun getProgressInfo(): String
        fun getProgress(): Float

        fun download(update: () -> Unit): Boolean

        fun remove()
    }

    class VideoCache(
        val type: String,
        val streamKeys: List<StreamKey>,
        val video: HttpUtil.HttpRequest,
        var contentLength: Long = 0L,
        var bytesDownloaded: Long = 0L,
        var percentDownloaded: Float = 0f
    ) : Cache {
        override fun isFinished(): Boolean = Math.abs(percentDownloaded - 100f) < 0.001f

        override fun getProgressInfo(): String =
            if (isFinished()) Formatter.formatFileSize(App.app.plugin, bytesDownloaded) else
                "${Formatter.formatFileSize(App.app.plugin, bytesDownloaded)}/${Formatter.formatFileSize(
                    App.app.plugin,
                    (bytesDownloaded * 100 / percentDownloaded).toLong()
                )}"

        override fun getProgress(): Float = percentDownloaded / 100f

        override fun download(update: () -> Unit): Boolean {
            var time = System.currentTimeMillis()
            createDownloader().download { contentLength, bytesDownloaded, percentDownloaded ->
                Log.v("download", "$contentLength, $bytesDownloaded, $percentDownloaded")
                this.contentLength = contentLength
                this.bytesDownloaded = bytesDownloaded
                this.percentDownloaded = percentDownloaded
                val now = System.currentTimeMillis()
                if (now - time > 1000) {
                    time = now
                    update()
                }
            }
            return !isFinished()
        }

        override fun remove() {
            createDownloader().remove()
        }

        private fun createDownloader(): Downloader {
            val dataSourceFactory = VideoModel.createDataSourceFactory(App.app.host, video, true)
            val downloaderFactory =
                DefaultDownloaderFactory(DownloaderConstructorHelper(App.app.downloadCache, dataSourceFactory))
            return downloaderFactory.createDownloader(
                DownloadRequest(
                    video.url,
                    type,
                    Uri.parse(video.url),
                    streamKeys,
                    null,
                    null
                )
            )
        }
    }

    class MangaCache(
        val images: List<MangaProvider.ImageInfo>,
        val request: HashMap<Int, HttpUtil.HttpRequest>,
        val paths: HashMap<Int, String>
    ) : Cache {
        override fun isFinished(): Boolean = paths.size >= images.size

        override fun getProgressInfo(): String = "${paths.size}/${images.size}"

        override fun getProgress(): Float = paths.size * 1f / images.size

        override fun download(update: () -> Unit): Boolean {
            images.forEachIndexed { index, image ->
                val req = request[index] ?: (App.app.lineProvider.getProvider(
                    Provider.TYPE_MANGA,
                    image.site ?: ""
                )?.provider as? MangaProvider)
                    ?.getImage("download_${image.site}_${image.id}", App.app.jsEngine, image)?.excute()
                ?: return@forEachIndexed
                request[index] = req
                val path = paths[index] ?: GlideUtil.with(App.app.host)?.downloadOnly()?.load(
                    GlideUrl(req.url) { if (!req.header.containsKey("referer")) req.header.plus("referer" to req.url) else req.header }
                )?.submit()?.get()?.absolutePath ?: return@forEachIndexed
                paths[index] = path
                update()
            }
            return !isFinished()
        }

        override fun remove() {
            paths.forEach {
                try {
                    File(it.value).delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}