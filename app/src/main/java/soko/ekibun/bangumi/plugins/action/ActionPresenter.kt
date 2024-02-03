package soko.ekibun.bangumi.plugins.action

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.widget.Toast
import soko.ekibun.bangumi.plugins.PluginPresenter
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ActionPresenter(activityRef: WeakReference<Activity>) : PluginPresenter(activityRef) {

    init {
        val activity = activityRef.get()!!
        val actionUrl = activity.intent.dataString
        Log.v("plugin", actionUrl!!)
        if (actionUrl?.startsWith("ekibun://bangumi/plugin") == true)
            subscribe {
                importProvider(activity, actionUrl)
                activity.finish()
            }
    }

    companion object {
        suspend fun addProvider(context: Context, providerInfo: ProviderInfo): Boolean {
            var resume = false
            return if (suspendCoroutine { continuation ->
                    val oldProvider = LineProvider.getProvider(providerInfo.type, providerInfo.site)
                    if (oldProvider != null)
                        AlertDialog.Builder(context)
                            .setMessage("接口 ${providerInfo.title}(${providerInfo.site}) 与现有接口 ${oldProvider.title}(${oldProvider.site}) 重复")
                            .setPositiveButton("替换") { _: DialogInterface, _: Int ->
                                resume = true
                                continuation.resume(true)
                            }.setNegativeButton("取消") { _: DialogInterface, _: Int -> }.setOnDismissListener {
                                if (!resume) continuation.resume(false)
                            }.show()
                    else continuation.resume(true)
                }) {
                LineProvider.addProvider(providerInfo)
                true
            } else false
        }

        suspend fun importProvider(context: Context, data: String): Boolean {
            return ProviderInfo.fromUrl(data)?.let { list ->
                Toast.makeText(context, "已添加${list.filter { addProvider(context, it) }.size}个接口", Toast.LENGTH_SHORT)
                    .show()
            } != null
        }
    }
}
