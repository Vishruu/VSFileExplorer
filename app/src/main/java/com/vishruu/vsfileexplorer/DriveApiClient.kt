package com.vishruu.vsfileexplorer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val modifiedTime: Long
) {
    val isFolder: Boolean
        get() = mimeType == "application/vnd.google-apps.folder"
}

object DriveApiClient {
    private const val BASE = "https://www.googleapis.com/drive/v3"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"

    private fun encode(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun parseTime(s: String?): Long {
        if (s.isNullOrEmpty()) return 0L
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .parse(s)?.time ?: 0L
        } catch (e: Exception) { 0L }
    }

    suspend fun listFiles(token: String, parentId: String = "root"): List<DriveFile> =
        withContext(Dispatchers.IO) {
            try {
                val q = encode("'$parentId' in parents and trashed = false")
                val fields = encode("files(id,name,mimeType,size,modifiedTime)")
                val url = "$BASE/files?q=$q&fields=$fields&pageSize=1000&orderBy=folder,name"
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 20000
                conn.readTimeout = 20000

                if (conn.responseCode != 200) return@withContext emptyList()

                val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(body)
                val arr = json.optJSONArray("files") ?: return@withContext emptyList()
                val result = mutableListOf<DriveFile>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    result.add(
                        DriveFile(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            mimeType = o.optString("mimeType", ""),
                            size = o.optString("size", "0").toLongOrNull() ?: 0L,
                            modifiedTime = parseTime(o.optString("modifiedTime", ""))
                        )
                    )
                }
                result
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun downloadFile(
        token: String,
        fileId: String,
        destFile: File,
        onProgress: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE/files/$fileId?alt=media"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            if (conn.responseCode != 200) return@withContext false

            val total = conn.contentLengthLong
            var downloaded = 0L
            conn.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buf = ByteArray(8192)
                    var n: Int
                    while (input.read(buf).also { n = it } > 0) {
                        output.write(buf, 0, n)
                        downloaded += n
                        if (total > 0) {
                            onProgress(((downloaded * 100) / total).toInt())
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadFile(
        context: Context,
        token: String,
        localFile: File,
        parentId: String = "root",
        onProgress: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val boundary = "----VSBoundary${System.currentTimeMillis()}"
            val metadata = JSONObject().apply {
                put("name", localFile.name)
                put("parents", org.json.JSONArray().put(parentId))
            }.toString()

            val url = URL("$UPLOAD/files?uploadType=multipart")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            conn.connectTimeout = 30000
            conn.readTimeout = 60000

            conn.outputStream.use { out ->
                // Metadata part
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
                out.write(metadata.toByteArray())
                out.write("\r\n".toByteArray())

                // Media part
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Type: application/octet-stream\r\n\r\n".toByteArray())

                val total = localFile.length()
                var sent = 0L
                localFile.inputStream().use { input ->
                    val buf = ByteArray(8192)
                    var n: Int
                    while (input.read(buf).also { n = it } > 0) {
                        out.write(buf, 0, n)
                        sent += n
                        if (total > 0) onProgress(((sent * 100) / total).toInt())
                    }
                }
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }

            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteFile(token: String, fileId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE/files/$fileId"
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.responseCode in 200..299 || conn.responseCode == 204
            } catch (e: Exception) {
                false
            }
        }

    suspend fun createFolder(
        token: String,
        name: String,
        parentId: String = "root"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("name", name)
                put("mimeType", "application/vnd.google-apps.folder")
                put("parents", org.json.JSONArray().put(parentId))
            }.toString()

            val conn = URL("$BASE/files").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toByteArray()) }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
}