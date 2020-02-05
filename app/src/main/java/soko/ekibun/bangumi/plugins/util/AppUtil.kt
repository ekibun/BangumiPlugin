package soko.ekibun.bangumi.plugins.util

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.util.Size
import android.view.WindowManager
import soko.ekibun.bangumi.plugins.bean.Subject


object AppUtil {
    fun parseSubjectActivityIntent(subject: Subject): Intent {
        val intent = Intent()
        intent.component = ComponentName("soko.ekibun.bangumi", "soko.ekibun.bangumi.ui.subject.SubjectActivity")
        intent.putExtra("extraSubject", JsonUtil.toJson(subject))
        return intent
    }

    fun getScreenSize(context: Context): Size{
        val p = Point()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(p)
        return Size(Math.min(p.x, p.y), Math.max(p.x, p.y))
    }

    fun shareString(context: Context, str: String){
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, str)
        intent.type = "text/plain"
        context.startActivity(Intent.createChooser(intent, "share"))
    }

    const val REQUEST_STORAGE_CODE = 1
    const val REQUEST_FILE_CODE = 2
    const val REQUEST_PROVIDER = 3
    fun checkStorage(context: Activity): Boolean{
        if (Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            context.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_STORAGE_CODE)
            return false
        }
        return true
    }
}