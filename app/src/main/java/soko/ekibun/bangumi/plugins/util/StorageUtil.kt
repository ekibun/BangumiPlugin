package soko.ekibun.bangumi.plugins.util

import android.content.Context
import android.os.Environment
import java.io.File

object StorageUtil{
    fun getDiskFileDir(context: Context, uniqueName: String): File {
        val filePath: String =
            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
                context.getExternalFilesDir(uniqueName)!!.absolutePath
            } else {
                context.filesDir.path + File.separator + uniqueName
            }
        return File(filePath)
    }
}