package soko.ekibun.bangumi.plugins.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
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

    private fun saveDrawableToPath(drawable: Drawable, path: String) {
        try {
            File(path).parentFile?.mkdirs()
            val stream = FileOutputStream(path, false) // overwrites this image every time
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 分享字符串
     * @param context Context
     * @param str String
     */
    fun shareString(context: Context, str: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, str)
        intent.type = "text/plain"
        context.startActivity(Intent.createChooser(intent, "分享"))
    }

    private fun deleteAllFiles(root: File) {
        val files = root.listFiles()
        if (files != null) for (f in files) {
            if (f.isDirectory) { // 判断是否为文件夹
                deleteAllFiles(f)
                try {
                    f.delete()
                } catch (e: java.lang.Exception) {
                }
            } else {
                if (f.exists()) { // 判断是否存在
                    deleteAllFiles(f)
                    try {
                        f.delete()
                    } catch (e: java.lang.Exception) {
                    }
                }
            }
        }
    }

    /**
     * 分享图片
     */
    fun shareDrawable(context: Context, drawable: Drawable) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs() // don't forget to make the directory
            deleteAllFiles(cachePath)
            val fileName = "image_${System.currentTimeMillis()}"
            saveDrawableToPath(drawable, "$cachePath/$fileName")
            val imageFile = File(cachePath, fileName)
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

    /**
     * 打开浏览器
     * @param context Context
     * @param url String
     */
    fun openBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 下载目录
     * @param context Context
     * @param uniqueName String
     * @return File
     */
    fun getDiskFileDir(context: Context, uniqueName: String): File {
        val filePath: String =
            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
                context.getExternalFilesDir(uniqueName)!!.absolutePath
            } else {
                context.filesDir.path + File.separator + uniqueName
            }
        return File(filePath)
    }

    const val REQUEST_PROVIDER = 3
}