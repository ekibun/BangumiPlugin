package soko.ekibun.bangumi.plugins.util

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import java.io.File

object StorageUtil{

    fun getDiskCacheDir(context: Context, uniqueName: String): File {
        val cachePath: String = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
            context.externalCacheDir!!.path
        } else {
            context.cacheDir.path
        }
        return File(cachePath + File.separator + uniqueName)
    }

    @SuppressLint("NewApi")
    fun getRealPathFromUri(context: Context, uri: Uri): String? {
        Log.v("uri", uri.toString())
        when {
            DocumentsContract.isDocumentUri(context, uri) -> {
                // 如果是document类型的 uri, 则通过document id来进行处理
                val documentId = DocumentsContract.getDocumentId(uri)
                when(uri.authority) {
                    "com.android.externalstorage.documents" -> {
                        val split = documentId.split(":")
                        if ("primary" == split[0]) {
                            return "${Environment.getExternalStorageDirectory()}/${split[1]}"
                        }
                    }
                    "com.android.providers.media.documents" -> { // MediaProvider
                        // 使用':'分割
                        val id = documentId.split(":")[1]
                        val selection = MediaStore.Video.Media._ID + "=?"
                        return getDataColumn(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, selection, arrayOf(id))
                    }
                    "com.android.providers.downloads.documents" -> { // DownloadsProvider
                        val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), documentId.toLong())
                        return getDataColumn(context, contentUri, null, null)
                    }
                }
            }
            "content" == uri.scheme -> // 如果是 content 类型的 Uri
                return getDataColumn(context, uri, null, null)
            "file" == uri.scheme -> // 如果是 file 类型的 Uri,直接获取图片对应的路径
                return uri.path
        }
        return null
    }

    private fun getDataColumn(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(projection[0])
                path = cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            cursor?.close()
        }
        return path
    }
}