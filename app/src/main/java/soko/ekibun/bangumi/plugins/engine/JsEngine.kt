package soko.ekibun.bangumi.plugins.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.commonjs.module.RequireBuilder
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.engine.module.AssetAndUrlModuleSourceProvider
import soko.ekibun.bangumi.plugins.ui.view.BackgroundWebView
import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

object JsEngine {
    private val webviewList = ConcurrentHashMap<String, BackgroundWebView>()

    private val scopeMap = ConcurrentHashMap<Thread, ScriptableObject>()

    private val polyfillScript by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val context = Context.getCurrentContext()
        context.compileReader(
            AssetAndUrlModuleSourceProvider.getAssetInputStream("polyfill.min.js"),
            "<polyfill>", 1, null
        )
    }
    private val globalMethods by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val context = Context.getCurrentContext()
        context.compileReader(
            AssetAndUrlModuleSourceProvider.getAssetInputStream("init.js"),
            "<init>", 1, null
        )
    }

    private fun initRequireBuilder(context: Context, scope: Scriptable) {
        val provider = AssetAndUrlModuleSourceProvider("modules", listOf<URI>(File("/").toURI()))
        RequireBuilder()
            .setModuleScriptProvider(SoftCachingModuleScriptProvider(provider))
            .setSandboxed(true)
            .createRequire(context, scope)
            .install(scope)
    }

    private fun runScript(script: String, header: String?, key: String): String {
        return try {
            val context = RhinoContextFactory.enterContext().also {
                it.wrapFactory.isJavaPrimitiveWrap = false
                it.optimizationLevel = -1
            }
            val sealedSharedScope = scopeMap.getOrPut(Thread.currentThread()) {
                val sharedScope = context.initStandardObjects()
                ScriptableObject.putConstProperty(sharedScope, "__webview_list__", webviewList)
                initRequireBuilder(context, sharedScope)
                polyfillScript.exec(context, sharedScope)
                globalMethods.exec(context, sharedScope)
                sharedScope.sealObject()
                sharedScope
            }
            val scope = context.newObject(sealedSharedScope)
            scope.prototype = sealedSharedScope
            scope.parentScope = null
            ScriptableObject.putProperty(scope, "__env_key__", key)
            if (!header.isNullOrEmpty()) context.evaluateString(scope, header, "header", 1, null)
            context.evaluateString(
                scope, """(function(){var _ret = (function(){$script
                |   }());
                |   return JSON.stringify(_ret);
                |}())""".trimMargin(), "script", 0, null
            ).toString()
        } finally {
            webviewList.remove(key)?.finish()
//            Context.exit()
        }
    }


    private fun <T> makeScriptImpl(
        script: String,
        header: String?,
        key: String,
        converter: (String) -> T
    ): T {
        return converter(runScript(script, header, key))
    }

    suspend fun <T> makeScript(
        script: String,
        header: String?,
        key: String,
        converter: (String) -> T
    ): T {
        return if (App.inited) withContext(Dispatchers.IO) { makeScriptImpl(script, header, key, converter) }
        else makeScriptImpl(script, header, key, converter)
    }
}