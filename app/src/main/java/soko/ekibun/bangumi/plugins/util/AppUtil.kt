package soko.ekibun.bangumi.plugins.util

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Size
import android.view.WindowManager
import androidx.core.content.FileProvider
import com.bumptech.glide.load.resource.gif.GifDrawable
import soko.ekibun.bangumi.plugins.bean.Subject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


object AppUtil {
    fun parseSubjectActivityIntent(subject: Subject): Intent {
        val intent = Intent()
        intent.component = ComponentName("soko.ekibun.bangumi", "soko.ekibun.bangumi.ui.subject.SubjectActivity")
        intent.putExtra("extraSubject", JsonUtil.toJson(subject))
        return intent
    }

    /**
     * 分享图片
     */
    fun shareDrawable(context: Context, drawable: Drawable) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs() // don't forget to make the directory
            val stream = FileOutputStream("$cachePath/image", false) // overwrites this image every time
            if (drawable is GifDrawable) {
                val newGifDrawable = (drawable.constantState!!.newDrawable().mutate()) as GifDrawable
                val byteBuffer = newGifDrawable.buffer
                val bytes = ByteArray(byteBuffer.capacity())
                (byteBuffer.duplicate().clear() as ByteBuffer).get(bytes)
                stream.write(bytes, 0, bytes.size)
            } else if (drawable is BitmapDrawable) {
                drawable.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            stream.close()

            val imageFile = File(cachePath, "image")
            val contentUri = FileProvider.getUriForFile(context, "soko.ekibun.bangumi.fileprovider", imageFile)

            if (contentUri != null) {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
                shareIntent.setDataAndType(contentUri, "image/*")
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                context.startActivity(Intent.createChooser(shareIntent, "分享图片"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getScreenSize(context: Context): Size {
        val p = Point()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(p)
        return Size(Math.min(p.x, p.y), Math.max(p.x, p.y))
    }

    fun shareString(context: Context, str: String) {
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