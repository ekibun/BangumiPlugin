package soko.ekibun.bangumi.plugins.util

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy

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
            else classLoader.loadClass(it.name)
        }.toTypedArray()
    }

    private fun getMethod(clazz: Class<*>, name: String, vararg params: Class<*>): Method? {
        val loaderParams = getLoaderClasses(clazz.classLoader!!, params)
        var type = clazz
        var ret: Method? = null
        do {
            try {
                ret = type.getDeclaredMethod(name, *loaderParams)
            } catch (e: NoSuchMethodException) {
            }
            if (ret != null) break
            type = type.superclass ?: break
        } while (true)
        ret?.isAccessible = true
        return ret
    }

    @SuppressLint("DefaultLocale")
    @Suppress("UNCHECKED_CAST")
    fun <T> proxyObject(obj: Any?, clazz: Class<T>): T? {
        if ((clazz as? Class<Any>)?.kotlin?.isData == true)
            return JsonUtil.toEntity(JsonUtil.toJson(obj), clazz) as? T
        if (clazz.classLoader == null || obj == null || obj.javaClass == clazz || !clazz.isInterface)
            return obj as? T
        return Proxy.newProxyInstance(
            clazz.classLoader, arrayOf(clazz)
        ) { _, method, args ->
            if (obj is Activity && View::class.java.isAssignableFrom(method.returnType))
                (obj as? Activity)?.findViewById(ResourceUtil.getId(obj, method.name.substring(3).toLowerCase()))
            else getMethod(obj.javaClass, method.name, *method.parameterTypes)?.let {
                it.invoke(obj, *(args ?: arrayOf()).mapIndexed { i, v ->
                    proxyObject(v, it.parameterTypes[i])
                }.toTypedArray())
            }?.let { proxyObject(it, method.returnType) }
        } as? T
    }

    fun <T : View> findViewById(view: View, id: String): T = view.findViewById(ResourceUtil.getId(view.context, id))

}