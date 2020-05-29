package soko.ekibun.bangumi.plugins.engine.module

import org.mozilla.javascript.commonjs.module.provider.ModuleSource
import soko.ekibun.bangumi.plugins.App
import java.io.*
import java.net.URI
import java.net.URISyntaxException
import java.net.URLConnection

/**
 * Created by Stardust on 2017/5/9.
 */
class AssetAndUrlModuleSourceProvider(
    private val mAssetDirPath: String,
    list: List<URI?>?
) : UrlModuleSourceProvider(list, null) {
    private val mBaseURI: URI = URI.create("file:///android_asset/$mAssetDirPath")

    @Throws(IOException::class, URISyntaxException::class)
    override fun loadFromPrivilegedLocations(
        moduleId: String,
        validator: Any?
    ): ModuleSource {
        var moduleIdWithExtension = moduleId
        if (!moduleIdWithExtension.endsWith(".js")) {
            moduleIdWithExtension += ".js"
        }
        return try {
            ModuleSource(
                getAssetInputStream("$mAssetDirPath/$moduleIdWithExtension"), null,
                URI("$mBaseURI/$moduleIdWithExtension"), mBaseURI, validator
            )
        } catch (e: FileNotFoundException) {
            super.loadFromPrivilegedLocations(moduleId, validator)
        }
    }

    @Throws(IOException::class)
    override fun getReader(urlConnection: URLConnection): Reader {
        val stream = urlConnection.getInputStream()
        val bytes = ByteArray(stream.available())
        stream.read(bytes)
        stream.close()
        return InputStreamReader(ByteArrayInputStream(bytes))
    }

    companion object {
        fun getAssetInputStream(fileName: String): InputStreamReader {
            return InputStreamReader(
                if (App.inited) App.app.plugin.assets.open(fileName)
                else File("./src/main/assets/${fileName}").inputStream()
            )
        }
    }
}