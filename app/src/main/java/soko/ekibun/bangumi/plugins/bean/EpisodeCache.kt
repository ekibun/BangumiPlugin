package soko.ekibun.bangumi.plugins.bean

import android.net.Uri
import android.text.format.Formatter
import com.bumptech.glide.load.model.GlideUrl
import com.google.android.exoplayer2.offline.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.model.VideoModel
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.book.BookProvider
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
        Provider.TYPE_BOOK -> JsonUtil.toEntity<BookCache>(cache)
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
            var time = 0L
            createDownloader().download { contentLength, bytesDownloaded, percentDownloaded ->
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

    class BookCache(
        val pages: List<BookProvider.PageInfo>,
        val request: HashMap<Int, HttpUtil.HttpRequest>,
        val paths: HashMap<Int, String>
    ) : Cache {
        override fun isFinished(): Boolean = paths.size >= pages.size

        override fun getProgressInfo(): String = "${paths.size}/${pages.size}"

        override fun getProgress(): Float = paths.size * 1f / pages.size

        override fun download(update: () -> Unit): Boolean {
            pages.forEachIndexed { index, page ->
                if (!page.content.isNullOrEmpty()) {
                    paths[index] = "content"
                    update()
                    return@forEachIndexed
                }
                val req = request[index] ?: (if (page.site.isNullOrEmpty()) page.image else null)
                ?: (App.app.lineProvider.getProvider(
                    Provider.TYPE_BOOK,
                    page.site ?: ""
                )?.provider as? BookProvider)
                    ?.getImage("download_${page.site}_${page.index}", App.app.jsEngine, page)?.execute()
                ?: return@forEachIndexed
                request[index] = req
                val header = req.header ?: HashMap()
                val path = paths[index] ?: GlideUtil.with(App.app.host)?.downloadOnly()?.load(
                    GlideUrl(req.url) { if (!header.containsKey("referer")) header.plus("referer" to req.url) else req.header }
                )?.submit()?.get()?.absolutePath ?: return@forEachIndexed
                paths[index] = path
                update()
            }
            update()
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