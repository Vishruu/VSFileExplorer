package com.vishruu.vsfileexplorer

import android.content.Context
import android.os.Environment
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URLDecoder
import java.util.Collections

object WifiTransferManager {
    private var server: FileServer? = null
    private const val PORT = 8080

    fun isRunning(): Boolean = server?.isAlive == true

    fun getUrl(): String? {
        if (!isRunning()) return null
        val ip = getLocalIpAddress() ?: return null
        return "http://$ip:$PORT"
    }

    fun start(context: Context): Boolean {
        if (isRunning()) return true
        return try {
            server = FileServer(context, PORT).also { it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false) }
            true
        } catch (e: Exception) {
            server = null
            false
        }
    }

    fun stop() {
        try { server?.stop() } catch (_: Exception) {}
        server = null
    }

    private fun getLocalIpAddress(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) { null }
    }
}

class FileServer(private val context: Context, port: Int) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"
        val method = session.method

        return try {
            when {
                method == Method.POST && uri == "/upload" -> handleUpload(session)
                uri == "/" || uri == "/index.html" -> htmlPage("/")
                uri.startsWith("/browse") -> {
                    val path = session.parameters["path"]?.firstOrNull() ?: "/"
                    htmlPage(path)
                }
                uri.startsWith("/download") -> {
                    val path = session.parameters["path"]?.firstOrNull() ?: return error("No path")
                    serveFile(path)
                }
                else -> error("Not found")
            }
        } catch (e: Exception) {
            error("Error: ${e.message}")
        }
    }

    private fun getRootDir(): File {
        return Environment.getExternalStorageDirectory()
    }

    private fun safeResolve(relPath: String): File? {
        val root = getRootDir()
        val clean = relPath.trimStart('/')
        val file = File(root, clean)
        return try {
            val rootCanonical = root.canonicalPath
            val fileCanonical = file.canonicalPath
            if (fileCanonical == rootCanonical || fileCanonical.startsWith(rootCanonical + File.separator)) {
                file
            } else null
        } catch (e: Exception) { null }
    }

    private fun htmlPage(path: String): Response {
        val dir = safeResolve(path) ?: return error("Invalid path")
        if (!dir.exists() || !dir.isDirectory) return error("Not a directory")

        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>VS File Explorer</title>")
        sb.append("<style>")
        sb.append("body{background:#0a0a0f;color:#f5f5f7;font-family:Arial,sans-serif;margin:0;padding:20px}")
        sb.append("h1{color:#9C27B0;margin-bottom:8px}")
        sb.append(".path{color:#9E9E9E;font-size:13px;margin-bottom:20px;word-break:break-all}")
        sb.append(".item{display:flex;align-items:center;padding:12px;background:#1a1a1f;border-radius:10px;margin-bottom:8px;text-decoration:none;color:#f5f5f7}")
        sb.append(".item:hover{background:#2a2a32}")
        sb.append(".icon{font-size:22px;margin-right:12px}")
        sb.append(".name{flex:1;word-break:break-all}")
        sb.append(".size{color:#9E9E9E;font-size:12px;margin-left:12px}")
        sb.append(".upload{background:#9C27B0;color:white;padding:12px 20px;border-radius:10px;border:none;cursor:pointer;font-size:15px;margin-bottom:20px;width:100%}")
        sb.append(".upload:hover{background:#7B1FA2}")
        sb.append("input[type=file]{display:none}")
        sb.append("</style></head><body>")
        sb.append("<h1>VS File Explorer</h1>")
        sb.append("<div class='path'>📁 ").append(dir.absolutePath).append("</div>")

        if (path != "/" && path.isNotEmpty()) {
            val parent = File(path).parent ?: "/"
            sb.append("<a class='item' href='/browse?path=").append(urlEncode(parent)).append("'>")
            sb.append("<span class='icon'>⬆️</span><span class='name'>.. Up</span></a>")
        }

        sb.append("<button class='upload' onclick=\"document.getElementById('f').click()\">⬆ Upload File</button>")
        sb.append("<form id='uf' method='post' action='/upload?path=").append(urlEncode(path)).append("' enctype='multipart/form-data'>")
        sb.append("<input id='f' type='file' name='file' onchange=\"document.getElementById('uf').submit()\"></form>")

        val children = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        for (child in children) {
            if (child.name.startsWith(".")) continue
            val childPath = path.trimEnd('/') + "/" + child.name
            if (child.isDirectory) {
                sb.append("<a class='item' href='/browse?path=").append(urlEncode(childPath)).append("'>")
                sb.append("<span class='icon'>📁</span><span class='name'>").append(escapeHtml(child.name)).append("</span></a>")
            } else {
                sb.append("<a class='item' href='/download?path=").append(urlEncode(childPath)).append("'>")
                sb.append("<span class='icon'>📄</span><span class='name'>").append(escapeHtml(child.name)).append("</span>")
                sb.append("<span class='size'>").append(formatSize(child.length())).append("</span></a>")
            }
        }

        sb.append("</body></html>")
        return newFixedLengthResponse(Response.Status.OK, "text/html", sb.toString())
    }

    private fun serveFile(path: String): Response {
        val file = safeResolve(path) ?: return error("Invalid path")
        if (!file.exists() || !file.isFile) return error("File not found")

        val mime = getMimeType(file.name)
        val fis = FileInputStream(file)
        val response = newChunkedResponse(Response.Status.OK, mime, fis)
        response.addHeader("Content-Disposition", "attachment; filename=\"${file.name}\"")
        response.addHeader("Content-Length", file.length().toString())
        return response
    }

    private fun handleUpload(session: IHTTPSession): Response {
        val path = session.parameters["path"]?.firstOrNull() ?: "/"
        val dir = safeResolve(path) ?: return error("Invalid path")
        if (!dir.isDirectory) return error("Not a directory")

        val files = HashMap<String, String>()
        try {
            session.parseBody(files)
        } catch (e: Exception) {
            return error("Parse error: ${e.message}")
        }

        for ((_, tmpPath) in files) {
            if (tmpPath.isEmpty()) continue
            val tmpFile = File(tmpPath)
            if (!tmpFile.exists()) continue
            val originalName = session.parameters["file"]?.firstOrNull()
                ?: tmpFile.name
            val safeName = File(originalName).name
            var dest = File(dir, safeName)
            if (dest.exists()) {
                val base = safeName.substringBeforeLast('.', safeName)
                val ext = if (safeName.contains('.')) "." + safeName.substringAfterLast('.') else ""
                var i = 1
                while (dest.exists()) {
                    dest = File(dir, "$base ($i)$ext")
                    i++
                }
            }
            tmpFile.copyTo(dest, overwrite = true)
            tmpFile.delete()
        }

        return newFixedLengthResponse(
            Response.Status.REDIRECT,
            "text/html",
            "<html><body><script>location.href='/browse?path=${urlEncode(path)}'</script></body></html>"
        ).apply { addHeader("Location", "/browse?path=${urlEncode(path)}") }
    }

    private fun urlEncode(s: String): String =
        java.net.URLEncoder.encode(s, "UTF-8")

    private fun escapeHtml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun error(msg: String): Response =
        newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", msg)

    private fun getMimeType(name: String): String {
        return when (name.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        return String.format("%.2f GB", mb / 1024.0)
    }
}