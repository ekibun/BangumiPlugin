package soko.ekibun.bangumi.plugins

import com.google.gson.JsonObject
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

    inline fun <reified T> getProvider(site: String): T {
        val obj = JsonObject()
        ReflectUtil.getAllFields(T::class.java).forEach {
            val file = File(
                "${SCRIPT_PATH}/${Provider.providers.toList()
                    .first { it.second == T::class.java }.first}/${site}/${it.name}.js"
            )
            if (!file.exists()) return@forEach
            obj.addProperty(it.name, file.readText())
        }
        return JsonUtil.toEntity(obj.toString(), T::class.java) as T
    }

}
