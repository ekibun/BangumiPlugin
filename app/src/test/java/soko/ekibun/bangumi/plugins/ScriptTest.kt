package soko.ekibun.bangumi.plugins

import com.google.gson.JsonObject
import soko.ekibun.bangumi.plugins.util.JsonUtil
import soko.ekibun.bangumi.plugins.util.ReflectUtil
import java.io.File

/**
 * 脚本测试类
 * @param T: Provider
 * @property site String
 * @property typeClass Class<T>
 * @constructor
 */
object ScriptTest {
    const val SCRIPT_PATH = "./src/test/java/soko/ekibun/bangumi/plugins/scripts"
    val jsEngine = JsEngine()

    inline fun <reified T> getProvider(site: String): T {
        val obj = JsonObject()
        ReflectUtil.getAllFields(T::class.java).forEach {
            val file = File("${SCRIPT_PATH}/${site}/${it.name}.js")
            if (!file.exists()) return@forEach
            obj.addProperty(it.name, file.readText())
        }
        return JsonUtil.toEntity(obj.toString(), T::class.java) as T
    }

//    @Test
//    fun saveProviderInfo(){
//        val file = File("${SCRIPT_PATH}/dist/${info.site}.json")
//        file.writeText(JsonUtil.toJson(info))
//    }
}
