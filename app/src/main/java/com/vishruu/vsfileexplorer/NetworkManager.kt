package com.vishruu.vsfileexplorer

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

data class RemoteFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val modified: Long
)

object NetworkManager {

    // ============ FTP ============
    private var ftpClient: FTPClient? = null
    private var ftpCurrent: NetworkConnection? = null

    fun ftpConnect(conn: NetworkConnection): String? {
        return try {
            disconnect()
            val client = FTPClient()
            client.connect(conn.host, conn.port)
            val reply = client.replyCode
            if (!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect()
                return "Server refused connection"
            }
            client.login(conn.username, conn.password)
            if (!FTPReply.isPositiveCompletion(client.replyCode)) {
                client.disconnect()
                return "Login failed"
            }
            client.enterLocalPassiveMode()
            client.setFileType(FTP.BINARY_FILE_TYPE)
            ftpClient = client
            ftpCurrent = conn
            null
        } catch (e: Exception) {
            disconnect()
            "FTP error: ${e.message}"
        }
    }

    fun ftpList(path: String): Pair<List<RemoteFile>?, String?> {
        val client = ftpClient ?: return null to "Not connected"
        return try {
            val files = client.listFiles(path) ?: return null to "Cannot list"
            val result = files.mapNotNull { f ->
                if (f.name == "." || f.name == "..") return@mapNotNull null
                RemoteFile(
                    name = f.name,
                    path = if (path.endsWith("/")) "$path${f.name}" else "$path/${f.name}",
                    isDirectory = f.isDirectory,
                    size = f.size,
                    modified = f.timestamp?.time?.time ?: 0L
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            result to null
        } catch (e: Exception) {
            null to "List error: ${e.message}"
        }
    }

    fun ftpDownload(remotePath: String, localFile: File): String? {
        val client = ftpClient ?: return "Not connected"
        return try {
            FileOutputStream(localFile).use { out ->
                val ok = client.retrieveFile(remotePath, out)
                if (!ok) return "Download failed"
            }
            null
        } catch (e: Exception) {
            "Download error: ${e.message}"
        }
    }

    fun ftpUpload(localFile: File, remotePath: String): String? {
        val client = ftpClient ?: return "Not connected"
        return try {
            localFile.inputStream().use { input ->
                val ok = client.storeFile(remotePath, input)
                if (!ok) return "Upload failed"
            }
            null
        } catch (e: Exception) {
            "Upload error: ${e.message}"
        }
    }

    fun ftpDelete(remotePath: String, isDirectory: Boolean): String? {
        val client = ftpClient ?: return "Not connected"
        return try {
            val ok = if (isDirectory) client.removeDirectory(remotePath)
            else client.deleteFile(remotePath)
            if (ok) null else "Delete failed"
        } catch (e: Exception) { "Delete error: ${e.message}" }
    }

    fun ftpMkdir(remotePath: String): String? {
        val client = ftpClient ?: return "Not connected"
        return try {
            if (client.makeDirectory(remotePath)) null else "Mkdir failed"
        } catch (e: Exception) { "Mkdir error: ${e.message}" }
    }

    // ============ SFTP ============
    private var sftpSession: Session? = null
    private var sftpChannel: ChannelSftp? = null

    fun sftpConnect(conn: NetworkConnection): String? {
        return try {
            disconnect()
            val jsch = JSch()
            val session = jsch.getSession(conn.username, conn.host, conn.port)
            session.setPassword(conn.password)
            val config = java.util.Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)
            session.connect(15000)
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(15000)
            sftpSession = session
            sftpChannel = channel
            null
        } catch (e: Exception) {
            disconnect()
            "SFTP error: ${e.message}"
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun sftpList(path: String): Pair<List<RemoteFile>?, String?> {
        val channel = sftpChannel ?: return null to "Not connected"
        return try {
            val entries = channel.ls(path) as List<ChannelSftp.LsEntry>
            val result = entries.mapNotNull { e ->
                val name = e.filename
                if (name == "." || name == "..") return@mapNotNull null
                val attrs = e.attrs
                RemoteFile(
                    name = name,
                    path = if (path.endsWith("/")) "$path$name" else "$path/$name",
                    isDirectory = attrs.isDir,
                    size = attrs.size,
                    modified = attrs.mTime.toLong() * 1000L
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            result to null
        } catch (e: Exception) {
            null to "SFTP list error: ${e.message}"
        }
    }

    fun sftpDownload(remotePath: String, localFile: File): String? {
        val channel = sftpChannel ?: return "Not connected"
        return try {
            channel.get(remotePath, FileOutputStream(localFile))
            null
        } catch (e: Exception) { "SFTP download error: ${e.message}" }
    }

    fun sftpUpload(localFile: File, remotePath: String): String? {
        val channel = sftpChannel ?: return "Not connected"
        return try {
            channel.put(localFile.absolutePath, remotePath)
            null
        } catch (e: Exception) { "SFTP upload error: ${e.message}" }
    }

    fun sftpDelete(remotePath: String, isDirectory: Boolean): String? {
        val channel = sftpChannel ?: return "Not connected"
        return try {
            if (isDirectory) channel.rmdir(remotePath) else channel.rm(remotePath)
            null
        } catch (e: Exception) { "SFTP delete error: ${e.message}" }
    }

    fun sftpMkdir(remotePath: String): String? {
        val channel = sftpChannel ?: return "Not connected"
        return try {
            channel.mkdir(remotePath)
            null
        } catch (e: Exception) { "SFTP mkdir error: ${e.message}" }
    }

    // ============ COMMON ============
    fun connect(conn: NetworkConnection): String? = when (conn.type) {
        NetworkType.FTP -> ftpConnect(conn)
        NetworkType.SFTP -> sftpConnect(conn)
    }

    fun list(path: String): Pair<List<RemoteFile>?, String?> = when (ftpCurrent?.type) {
        NetworkType.FTP -> ftpList(path)
        NetworkType.SFTP -> sftpList(path)
        else -> null to "Not connected"
    }

    fun download(remotePath: String, localFile: File): String? = when (ftpCurrent?.type) {
        NetworkType.FTP -> ftpDownload(remotePath, localFile)
        NetworkType.SFTP -> sftpDownload(remotePath, localFile)
        else -> "Not connected"
    }

    fun upload(localFile: File, remotePath: String): String? = when (ftpCurrent?.type) {
        NetworkType.FTP -> ftpUpload(localFile, remotePath)
        NetworkType.SFTP -> sftpUpload(localFile, remotePath)
        else -> "Not connected"
    }

    fun delete(remotePath: String, isDirectory: Boolean): String? = when (ftpCurrent?.type) {
        NetworkType.FTP -> ftpDelete(remotePath, isDirectory)
        NetworkType.SFTP -> sftpDelete(remotePath, isDirectory)
        else -> "Not connected"
    }

    fun mkdir(remotePath: String): String? = when (ftpCurrent?.type) {
        NetworkType.FTP -> ftpMkdir(remotePath)
        NetworkType.SFTP -> sftpMkdir(remotePath)
        else -> "Not connected"
    }

    fun disconnect() {
        try { ftpClient?.logout(); ftpClient?.disconnect() } catch (_: Exception) {}
        ftpClient = null
        try { sftpChannel?.disconnect() } catch (_: Exception) {}
        sftpChannel = null
        try { sftpSession?.disconnect() } catch (_: Exception) {}
        sftpSession = null
        ftpCurrent = null
    }

    fun isConnected(): Boolean = ftpClient?.isConnected == true || sftpChannel?.isConnected == true

    fun formatRemoteSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        return String.format("%.2f GB", mb / 1024.0)
    }

    fun formatRemoteDate(millis: Long): String {
        if (millis <= 0) return ""
        return try {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(java.util.Date(millis))
        } catch (e: Exception) { "" }
    }
}