package soko.ekibun.bangumi.plugins.util

import android.util.Log
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.ArrayList

object ReflectUtil {
    fun getAllFields(clazz: Class<*>): List<Field> {
        val fields = ArrayList<Field>()
        var type = clazz
        do{
            fields.addAll(type.declaredFields)
            type = type.superclass?:break
        } while(true)
        return fields
    }

    fun <T> newInstance(clazz: Class<T>): T {
        return JsonUtil.toEntity<T>("{}", clazz)!!
    }

    fun getMethod(clazz: Class<*>, name: String, vararg params: Class<*>): Method? {
        var type = clazz
        var ret: Method? = null
        do{
            Log.v("plugin", type.name)
            type.declaredMethods.forEach {
                Log.v("plugin", it.toGenericString())
            }
            try{
                ret = type.getDeclaredMethod(name, *params)
            }catch (e: NoSuchMethodException){  }
            if(ret != null) break
            type = type.superclass?:break
        } while(true)
        ret?.isAccessible = true
        return ret
    }

    fun getVal(obj: Any, name: String): Any? {
        return obj.javaClass.getDeclaredMethod("get" + String(name.toCharArray().let {
            it[0] = it[0].toUpperCase(); it
        })).let {
            it.isAccessible = true
            it.invoke(obj)
        }
    }

    private fun getLoaderClasses(classLoader: ClassLoader, classes: Array<out Class<*>>): Array<Class<*>> {
        return classes.map {
            if(it in arrayOf(Int::class.java)) it
            else classLoader.loadClass(it.name)
        }.toTypedArray()
    }

    fun invoke(obj: Any, method: String, params: Array<out Class<*>>, vararg args: Any?) {
        obj.javaClass.getMethod(
            method,
            *getLoaderClasses(obj.javaClass.classLoader!!, params)
        ).invoke(obj, *(args.map { convertFunction(obj.javaClass.classLoader!!, it) }.toTypedArray()))
    }

    inline fun <reified T> convert(obj: Any?): T? {
        if(obj == null) return obj
        return JsonUtil.toEntity<T>(JsonUtil.toJson(obj))
    }

    fun convertFunction(classLoader: ClassLoader, obj: Any?): Any? {
        if (obj !is Function<*>) return obj
        return Proxy.newProxyInstance(
            classLoader, getLoaderClasses(classLoader, obj.javaClass.interfaces)
        ) { _, method, args ->
            obj.javaClass.getMethod(
                method.name,
                *getLoaderClasses(obj.javaClass.classLoader!!, method.parameterTypes)
            ).invoke(obj, *args)
        }
    }
}