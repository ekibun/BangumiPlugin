package soko.ekibun.bangumi.plugins.model

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import android.view.SurfaceView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.StreamKey
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.util.Util
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.bean.Episode
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.model.cache.EpisodeCache
import soko.ekibun.bangumi.plugins.model.line.LineInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.music.MusicPluginView
import soko.ekibun.bangumi.plugins.provider.music.MusicProvider
import soko.ekibun.bangumi.plugins.provider.video.VideoProvider
import soko.ekibun.bangumi.plugins.util.NetworkUtil
import java.text.DecimalFormat

object VideoModel {
    const val ACTION_MEDIA_CONTROL = "soko.ekibun.bangumi.plugin.video.mediaControl"
    const val EXTRA_CONTROL_TYPE = "extraControlType"
    const val CONTROL_TYPE_PAUSE = 1
    const val CONTROL_TYPE_PLAY = 2
    const val CONTROL_TYPE_NEXT = 3
    const val CONTROL_TYPE_PREV = 4
    const val CONTROL_TYPE_REPEAT = 5

    interface Listener {
        fun onReady(playWhenReady: Boolean)
        fun onBuffering()
        fun onEnded()
        fun onVideoSizeChange(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float)
        fun onError(error: ExoPlaybackException)
    }

    data class StoreState(
        val pluginView: MusicPluginView,
        val data: MusicPluginView.MusicStoreData?
    )

    var listener: Listener? = null
    var lastState: StoreState? = null
    fun attachToActivity(onAction: Listener) {
        listener = onAction
    }

    fun detachToActivity(onAction: Listener) {
        if (listener == onAction) listener = null
    }

    var cover: Bitmap? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                CONTROL_TYPE_PLAY -> {
                    lastState?.pluginView?.doPlayPause(true)
                }
                CONTROL_TYPE_PAUSE -> {
                    lastState?.pluginView?.doPlayPause(false)
                }
                CONTROL_TYPE_NEXT -> {
                    lastState?.pluginView?.nextEpisode?.invoke()?.let { lastState?.pluginView?.loadEp(it) }
                }
                CONTROL_TYPE_PREV -> {
                    lastState?.pluginView?.prevEpisode?.invoke()?.let { lastState?.pluginView?.loadEp(it) }
                }
                CONTROL_TYPE_REPEAT -> {
                    lastState?.pluginView?.changeRepeat()
                }
            }
        }
    }

    val player: SimpleExoPlayer by lazy {
        val player = SimpleExoPlayer.Builder(App.app.host).build()
        player.addListener(object : Player.EventListener {
            override fun onSeekProcessed() {}
            override fun onPlayerError(error: ExoPlaybackException) {
                listener?.onError(error)
            }

            override fun onLoadingChanged(isLoading: Boolean) {}
            override fun onPositionDiscontinuity(reason: Int) {}
            override fun onRepeatModeChanged(repeatMode: Int) {}
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

            @SuppressLint("SwitchIntDef")
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> listener?.onEnded()
                    Player.STATE_READY -> listener?.onReady(playWhenReady)
                    Player.STATE_BUFFERING -> listener?.onBuffering()
                }
            }
        })
        player.addVideoListener(object : com.google.android.exoplayer2.video.VideoListener {
            override fun onVideoSizeChanged(
                width: Int,
                height: Int,
                unappliedRotationDegrees: Int,
                pixelWidthHeightRatio: Float
            ) {
                listener?.onVideoSizeChange(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
            }

            override fun onRenderedFirstFrame() {}
        })
        player
    }

    private fun createMediaSource(request: Provider.HttpRequest, streamKeys: List<StreamKey>? = null): MediaSource {
        val uri = Uri.parse(request.url)
        val dataSourceFactory = createDataSourceFactory(App.app.host, request, streamKeys != null)
        return when (@C.ContentType Util.inferContentType(uri, request.overrideExtension)) {
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory)
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory)
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory)
            else -> ProgressiveMediaSource.Factory(dataSourceFactory)
        }.let {
            if (streamKeys != null) it.setStreamKeys(streamKeys)
            it
        }.createMediaSource(uri)
    }

    fun play(request: Provider.HttpRequest, surface: SurfaceView? = null, streamKeys: List<StreamKey>? = null) {
        if (surface != null) {
            player.setVideoSurfaceView(surface)
            player.repeatMode = Player.REPEAT_MODE_OFF
        } else {
            App.app.host.registerReceiver(
                receiver,
                IntentFilter(ACTION_MEDIA_CONTROL + lastState?.pluginView?.linePresenter?.subject?.id)
            )
        }
        player.prepare(createMediaSource(request, streamKeys))
        player.playWhenReady = true
    }

    fun createDataSourceFactory(
        context: Context,
        request: Provider.HttpRequest,
        useCache: Boolean = false
    ): DefaultDataSourceFactory {
        val header = request.header ?: HashMap()
        val httpSourceFactory = DefaultHttpDataSourceFactory(
            header["User-Agent"] ?: "exoplayer",
            null,
            DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
            DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
            true
        )
        header.forEach {
            httpSourceFactory.defaultRequestProperties.set(it.key, it.value)
        }
        return DefaultDataSourceFactory(
            context,
            null,
            if (useCache) CacheDataSourceFactory(
                App.app.downloadCache,
                httpSourceFactory
            ) else httpSourceFactory
        )
    }

    //private val videoCacheModel by lazy{ App.getVideoCacheModel(content)}
    suspend fun getVideo(
        key: String, subject: Subject, episode: Episode, info: LineInfo?,
        onGetVideoInfo: (VideoProvider.VideoInfo) -> Unit,
        networkChecker: suspend () -> Unit
    ): Pair<Provider.HttpRequest?, List<StreamKey>?> {
        //val videoCache = videoCacheModel.getCache(episode, subject)
        val videoCache =
            EpisodeCacheModel.getEpisodeCache(episode, subject)?.cache() as? EpisodeCache.VideoCache
        if (videoCache != null) {
            onGetVideoInfo(VideoProvider.VideoInfo("", videoCache.video.url, videoCache.video.url))
            return videoCache.video to videoCache.streamKeys
        } else {
            val provider = LineProvider.getProvider(
                Provider.TYPE_VIDEO,
                info?.site ?: ""
            )?.provider as? VideoProvider
            if (info == null || provider == null) {
                if (info?.site == "") {
                    val format =
                        (Regex("""\{\{(.*)\}\}""").find(info.id)?.groupValues ?: listOf(
                            "{{ep}}",
                            "ep"
                        )).toMutableList()
                    if (format[0] == "{{ep}}") format[1] = "#.##"
                    val url = try {
                        info.id.replace(format[0], DecimalFormat(format[1]).format(episode.sort))
                    } catch (e: Exception) {
                        info.id
                    }
                    onGetVideoInfo(VideoProvider.VideoInfo("", url, url))
                    return Provider.HttpRequest(url) to null
                }
            } else {
                val video = provider.getVideoInfo(key, info, episode)
                onGetVideoInfo(video)
                return if (video.site == "") Provider.HttpRequest(video.url) to null
                else {
                    if (!NetworkUtil.isWifiConnected(App.app.host)) networkChecker()
                    val videoProvider = LineProvider.getProvider(
                        Provider.TYPE_VIDEO,
                        video.site
                    )?.provider as VideoProvider
                    videoProvider.getVideo(key, video) to null
                }
            }
        }
        return null to null
    }

    suspend fun getMusic(
        key: String, subject: Subject, episode: Episode, info: LineInfo?
    ): Pair<Provider.HttpRequest?, List<StreamKey>?> {
        //val videoCache = videoCacheModel.getCache(episode, subject)
        val videoCache =
            EpisodeCacheModel.getEpisodeCache(episode, subject)?.cache() as? EpisodeCache.VideoCache
        if (videoCache != null) {
            return videoCache.video to videoCache.streamKeys
        } else {
            val provider = LineProvider.getProvider(
                Provider.TYPE_MUSIC,
                info?.site ?: ""
            )?.provider as? MusicProvider
            if (info == null || provider == null) {
                if (info?.site == "") {
                    val format =
                        (Regex("""\{\{(.*)\}\}""").find(info.id)?.groupValues ?: listOf(
                            "{{ep}}",
                            "ep"
                        )).toMutableList()
                    if (format[0] == "{{ep}}") format[1] = "#.##"
                    val url = try {
                        info.id.replace(format[0], DecimalFormat(format[1]).format(episode.sort))
                    } catch (e: Exception) {
                        info.id
                    }
                    return Provider.HttpRequest(url) to null
                }
            } else {
                return provider.getMusic(key, episode.provider ?: return null to null) to null
            }
        }
        return null to null
    }

    fun createDownloadRequest(request: Provider.HttpRequest, callback: DownloadHelper.Callback) {
        val uri = Uri.parse(request.url)
        val dataSourceFactory = createDataSourceFactory(App.app.host, request, true)
        val helper = when (@C.ContentType Util.inferContentType(uri, request.overrideExtension)) {
            C.TYPE_DASH -> DownloadHelper.forDash(
                App.app.host,
                uri,
                dataSourceFactory,
                DefaultRenderersFactory(App.app.host)
            )
            C.TYPE_SS -> DownloadHelper.forSmoothStreaming(
                App.app.host,
                uri,
                dataSourceFactory,
                DefaultRenderersFactory(App.app.host)
            )
            C.TYPE_HLS -> DownloadHelper.forHls(
                App.app.host,
                uri,
                dataSourceFactory,
                DefaultRenderersFactory(App.app.host)
            )
            else -> DownloadHelper.forProgressive(App.app.host, uri)
        }
        helper.prepare(callback)
    }
}