package soko.ekibun.bangumi.plugins.util

import android.app.Activity
import android.view.View
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import kotlin.collections.ArrayList

object ReflectUtil {
    fun getAllFields(clazz: Class<*>): List<Field> {
        val fields = ArrayList<Field>()
        var type = clazz
        do {
            fields.addAll(type.declaredFields)
            type = type.superclass ?: break
        } while (true)
        return fields
    }

    private fun getLoaderClasses(classLoader: ClassLoader, classes: Array<out Class<*>>): Array<Class<*>> {
        return classes.map {
            if (it.isPrimitive || it.classLoader == classLoader) it
            else classLoader.loadClass(it.getAnnotation(ReflectClass::class.java)?.name ?: it.name)
        }.toTypedArray()
    }

    //    private val methodCache = HashMap<Class<*>, HashMap<Method, Method>>()
    private fun getMethod(clazz: Class<*>, method: Method): Method? {
//        val cache = methodCache[clazz]?.get(method)
//        if (cache != null) return cache
        val loaderParams = getLoaderClasses(clazz.classLoader!!, method.parameterTypes)
        var type = clazz
        var ret: Method? = null
        do {
            try {
                ret = type.getDeclaredMethod(method.name, *loaderParams)
            } catch (e: NoSuchMethodException) {
            }
            if (ret != null) break
            type = type.superclass ?: break
        } while (true)
        ret?.isAccessible = true
//        if(ret != null) methodCache.getOrPut(clazz) { HashMap() }[method] = ret
        return ret
    }

    @Target(AnnotationTarget.CLASS)
    annotation class ReflectClass(val name: String)

    /**
     * 判断是不是Data类型
     */
    private fun isDataClass(clazz: Class<*>?): Boolean {
        return try {
            clazz?.classLoader == ReflectUtil::class.java.classLoader && clazz?.kotlin?.isData ?: false
        } catch (e: Throwable) {
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> proxyObjectInternal(objRef: () -> Any?, clazz: Class<T>): T? {
        val refObj = objRef()
        if (isDataClass(clazz) || isDataClass(refObj?.javaClass))
            return JsonUtil.toEntity(JsonUtil.toJson(refObj), clazz)
        if (clazz.classLoader == null || refObj == null || refObj.javaClass == clazz || !clazz.isInterface)
            return refObj as? T
        val objectCache = HashMap<Method, Any>()
        return Proxy.newProxyInstance(
            clazz.classLoader, arrayOf(clazz)
        ) { _, method, args ->
            val obj = objRef() ?: return@newProxyInstance null
            if (obj is Activity && View::class.java.isAssignableFrom(method.returnType))
                (obj as? Activity)?.findViewById(ResourceUtil.getId(obj, method.name.substring(3).toLowerCase()))
            else getMethod(obj.javaClass, method)?.let {
                it.invoke(obj, *(args ?: arrayOf()).mapIndexed { i, v ->
                    proxyObject(v, it.parameterTypes[i])
                }.toTypedArray())
            }?.let { proxyObject(it, method.returnType) }?.also {
                if (method.name.startsWith("get")) {
                    objectCache[method] = it
                }
            }
        } as? T
    }

    fun <T> proxyObject(obj: Any?, clazz: Class<T>): T? {
        return proxyObjectInternal({ obj }, clazz)
    }

    fun <T> proxyObjectWeak(objRef: WeakReference<*>, clazz: Class<T>): T? {
        return proxyObjectInternal({ objRef.get() }, clazz)
    }

    fun <T : View> findViewById(view: View, id: String): T = view.findViewById(ResourceUtil.getId(view.context, id))

}