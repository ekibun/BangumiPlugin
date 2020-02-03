package soko.ekibun.bangumi.plugins.provider

import soko.ekibun.bangumi.plugins.JsEngine
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.model.LineInfoModel
import soko.ekibun.bangumi.plugins.util.JsonUtil

abstract class Provider(
    @Code("搜索",  0) val search: String? = null
){

    fun search(scriptKey: String, jsEngine: JsEngine, key: String): JsEngine.ScriptTask<List<LineInfoModel.LineInfo>>{
        return JsEngine.ScriptTask(jsEngine,"var key = ${JsonUtil.toJson(key)};\n$search", scriptKey){
            JsonUtil.toEntity<List<LineInfoModel.LineInfo>>(it)?:ArrayList()
        }
    }

    @Target(AnnotationTarget.FIELD)
    annotation class Code(val label: String, val index: Int)

    companion object {
        val providers: Map<String, Class<out Provider>> = mapOf(
            "video" to VideoProvider::class.java
        )

        fun getProviderType(subject: Subject): String {
            return when(subject.type){
                Subject.TYPE_ANIME, Subject.TYPE_REAL -> "video"
                else -> ""
            }
        }
    }
}