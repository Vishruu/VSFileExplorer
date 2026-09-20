package com.vishruu.vsfileexplorer

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class TrashEntry(
    val trashPath: String,
    val originalPath: String,
    val name: String,
    val deletedAt: Long,
    val size: Long,
    val isDirectory: Boolean
)

object TrashStore {
    private const val TRASH_DIR = ".vs_trash"
    private const val INDEX_FILE = "index.json"
    private const val AUTO_CLEAN_DAYS = 30

    private fun trashDir(): File {
        val dir = File(
            Environment.getExternalStorageDirectory(),
            "VSFileExplorer/$TRASH_DIR"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun indexFile(): File = File(trashDir(), INDEX_FILE)

    private fun loadIndex(): MutableList<TrashEntry> {
        val list = mutableListOf<TrashEntry>()
        try {
            val f = indexFile()
            if (!f.exists()) return list
            val json = JSONArray(f.readText())
            for (i in 0 until json.length()) {
                val o = json.getJSONObject(i)
                list.add(
                    TrashEntry(
                        trashPath = o.getString("trashPath"),
                        originalPath = o.getString("originalPath"),
                        name = o.getString("name"),
                        deletedAt = o.getLong("deletedAt"),
                        size = o.getLong("size"),
                        isDirectory = o.getBoolean("isDirectory")
                    )
                )
            }
        } catch (e: Exception) { }
        return list
    }

    private fun saveIndex(list: List<TrashEntry>) {
        try {
            val arr = JSONArray()
            list.forEach { e ->
                val o = JSONObject()
                o.put("trashPath", e.trashPath)
                o.put("originalPath", e.originalPath)
                o.put("name", e.name)
                o.put("deletedAt", e.deletedAt)
                o.put("size", e.size)
                o.put("isDirectory", e.isDirectory)
                arr.put(o)
            }
            indexFile().writeText(arr.toString())
        } catch (e: Exception) { }
    }

    fun getEntries(): List<TrashEntry> {
        autoClean()
        return loadIndex().sortedByDescending { it.deletedAt }
    }

    fun moveToTrash(file: File): Boolean {
        return try {
            val dir = trashDir()
            val timestamp = System.currentTimeMillis()
            val safeName = "${timestamp}_${file.name}"
            val dest = File(dir, safeName)

            if (file.isDirectory) {
                copyDirectoryTo(file, dest)
                deleteRecursive(file)
            } else {
                file.copyTo(dest, overwrite = true)
                file.delete()
            }

            val list = loadIndex()
            list.add(
                TrashEntry(
                    trashPath = dest.absolutePath,
                    originalPath = file.absolutePath,
                    name = file.name,
                    deletedAt = timestamp,
                    size = if (file.isDirectory) 0L else file.length(),
                    isDirectory = file.isDirectory
                )
            )
            saveIndex(list)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun restore(entry: TrashEntry): Boolean {
        return try {
            val src = File(entry.trashPath)
            if (!src.exists()) return false
            val original = File(entry.originalPath)
            original.parentFile?.mkdirs()

            // if original exists, add suffix
            val finalDest = if (original.exists()) {
                getUniqueFile(original.parentFile ?: File("/"), original.name)
            } else original

            if (src.isDirectory) {
                copyDirectoryTo(src, finalDest)
                deleteRecursive(src)
            } else {
                src.copyTo(finalDest, overwrite = true)
                src.delete()
            }

            val list = loadIndex().toMutableList()
            list.removeAll { it.trashPath == entry.trashPath }
            saveIndex(list)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deletePermanently(entry: TrashEntry): Boolean {
        return try {
            val src = File(entry.trashPath)
            if (src.exists()) deleteRecursive(src)
            val list = loadIndex().toMutableList()
            list.removeAll { it.trashPath == entry.trashPath }
            saveIndex(list)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun emptyAll(): Int {
        val list = loadIndex()
        var count = 0
        list.forEach { e ->
            try {
                val f = File(e.trashPath)
                if (f.exists()) deleteRecursive(f)
                count++
            } catch (ex: Exception) { }
        }
        saveIndex(emptyList())
        return count
    }

    private fun autoClean() {
        try {
            val cutoff = System.currentTimeMillis() - (AUTO_CLEAN_DAYS.toLong() * 24 * 60 * 60 * 1000)
            val list = loadIndex().toMutableList()
            val toRemove = list.filter { it.deletedAt < cutoff }
            toRemove.forEach { e ->
                try {
                    val f = File(e.trashPath)
                    if (f.exists()) deleteRecursive(f)
                } catch (ex: Exception) { }
            }
            if (toRemove.isNotEmpty()) {
                list.removeAll(toRemove)
                saveIndex(list)
            }
        } catch (e: Exception) { }
    }

    private fun copyDirectoryTo(src: File, dest: File) {
        if (!dest.exists()) dest.mkdirs()
        src.listFiles()?.forEach { child ->
            val newChild = File(dest, child.name)
            if (child.isDirectory) copyDirectoryTo(child, newChild)
            else child.copyTo(newChild, overwrite = true)
        }
    }

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }
}