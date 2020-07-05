package soko.ekibun.bangumi.plugins.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GzipUtil {
    const val GZIP_ENCODE_UTF_8 = "UTF-8"

    /**
     * 字符串压缩为GZIP字节数组
     * @param str
     * @param encoding
     * @return
     */
    @Throws(IOException::class)
    fun compress(str: String?, encoding: String = GZIP_ENCODE_UTF_8): ByteArray? {
        if (str == null || str.isEmpty()) {
            return null
        }
        val out = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(out)
        gzip.write(str.toByteArray(charset(encoding)))
        gzip.close()
        return out.toByteArray()
    }

    /**
     * Gzip  byte[] 解压成字符串
     * @param bytes
     * @param encoding
     * @return
     */
    @Throws(IOException::class)
    fun uncompressToString(
        bytes: ByteArray?,
        encoding: String = GZIP_ENCODE_UTF_8
    ): String? {
        if (bytes == null || bytes.isEmpty()) {
            return null
        }
        val out = ByteArrayOutputStream()
        val `in` = ByteArrayInputStream(bytes)
        val ungzip = GZIPInputStream(`in`)
        val buffer = ByteArray(256)
        var n: Int
        while (ungzip.read(buffer).also { n = it } >= 0) {
            out.write(buffer, 0, n)
        }
        return out.toString(encoding)
    }
}