package soko.ekibun.bangumi.plugins

import com.google.gson.JsonObject
import org.junit.Test
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.util.JsonUtil
import soko.ekibun.bangumi.plugins.util.ReflectUtil
import java.io.File

/**
 * 脚本测试类
 * @constructor
 */
object ScriptTest {
    const val SCRIPT_PATH = "./src/test/java/soko/ekibun/bangumi/plugins/scripts"

    @Suppress("UNCHECKED_CAST")
    fun <T : Provider> getProvider(provider: BaseTestData<T>): T {
        val obj = JsonObject()
        ReflectUtil.getAllFields(provider.providerClass).forEach {
            val file = File(
                "${SCRIPT_PATH}/${Provider.providers.toList()
                    .first { it.second == provider.providerClass }.first}/${provider.info.site}/${it.name}.js"
            )
            if (!file.exists()) return@forEach
            obj.addProperty(it.name, file.readText())
        }
        return JsonUtil.toEntity(obj.toString(), provider.providerClass) as T
    }

    interface BaseTestData<T : Provider> {
        val info: ProviderInfo
        val providerClass: Class<T>
    }

    class WriteProvider {
        @Test
        fun writeProvider() {
            val file = File("${SCRIPT_PATH}/providers.html")
            val providers = scriptList.map {
                it.info.code = JsonUtil.toJson(getProvider(it))
                it.info
            }
            file.writeText("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><meta charset=\"UTF-8\">\n" +
                    "<h1><a href=\"${ProviderInfo.toUrl(providers)}\">线路合集</a></h1>\n" +
                    mapOf(
                        "视频" to Provider.TYPE_VIDEO,
                        "书籍" to Provider.TYPE_BOOK,
                        "音乐" to Provider.TYPE_MUSIC
                    ).toList().joinToString("\n") { (cat, type) ->
                        providers.filter { it.type == type }.let { typeProviders ->
                            "<h2><a href=\"${ProviderInfo.toUrl(typeProviders)})\">$cat</a></h2>" + typeProviders.joinToString(
                                "\n"
                            ) {
                                "<li><a href=\"${ProviderInfo.toUrl(listOf(it))}\">${it.title}</a></li>"
                            }
                        }
                    }
            )
        }

        private val scriptList = arrayOf(
            // book
            soko.ekibun.bangumi.plugins.scripts.book.dmzj.TestData(),
            soko.ekibun.bangumi.plugins.scripts.book.hanhan.TestData(),
            soko.ekibun.bangumi.plugins.scripts.book.mangabz.TestData(),
            soko.ekibun.bangumi.plugins.scripts.book.manhua123.TestData(),
            soko.ekibun.bangumi.plugins.scripts.book.manhuabei.TestData(),
            soko.ekibun.bangumi.plugins.scripts.book.manhuagui.TestData(),
            soko.ekibun.bangumi.plugins.scripts.book.mh177.TestData(),
            soko.ekibun.bangumi.plugins.scripts.book.ohmanhua.TestData(),
            soko.ekibun.bangumi.plugins.scripts.book.pica.TestData(),
            soko.ekibun.bangumi.plugins.scripts.book.wenku8.TestData(),
            // video
            soko.ekibun.bangumi.plugins.scripts.video.acfun.TestData(),
            soko.ekibun.bangumi.plugins.scripts.video.agefans.TestData(),
            soko.ekibun.bangumi.plugins.scripts.video.bilibili.TestData(),
            soko.ekibun.bangumi.plugins.scripts.video.fodm.TestData(),
            soko.ekibun.bangumi.plugins.scripts.video.hehua.TestData(),
            soko.ekibun.bangumi.plugins.scripts.video.iqiyi.TestData(),
            soko.ekibun.bangumi.plugins.scripts.video.nicotv.TestData(),
            soko.ekibun.bangumi.plugins.scripts.video.ningmoe.TestData(),
            soko.ekibun.bangumi.plugins.scripts.video.pptv.TestData(),
            soko.ekibun.bangumi.plugins.scripts.video.tencent.TestData(),
            soko.ekibun.bangumi.plugins.scripts.video.webpage.TestData(),
            //music
            soko.ekibun.bangumi.plugins.scripts.music.netease.TestData()
        )
    }
}
