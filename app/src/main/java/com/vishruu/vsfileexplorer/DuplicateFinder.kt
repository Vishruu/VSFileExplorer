package com.vishruu.vsfileexplorer

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

object DuplicateFinder {

    data class DupGroup(
        val hash: String,
        val size: Long,
        val files: List<File>
    ) {
        val wastedSpace: Long
            get() = size * (files.size - 1).coerceAtLeast(0)
    }

    data class ScanResult(
        val groups: List<DupGroup>,
        val totalFilesScanned: Int,
        val totalGroups: Int,
        val totalDuplicates: Int,
        val totalWasted: Long
    )

    suspend fun scan(
        root: File = Environment.getExternalStorageDirectory(),
        onProgress: (Int) -> Unit = {},
        onStatus: (String) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): ScanResult = coroutineScope {

        onProgress(1)

        // ============ PHASE 1: Collect files (1% → 20%) ============
        onStatus("Collecting files…")

        val allFiles = mutableListOf<File>()
        val skipFolders = setOf(
            "Android/data", "Android/obb",
            "VSFileExplorer/.vs_trash", "VSFileExplorer/.vault_cache",
            "VSFileExplorer/Vault",
            ".thumbnails"
        )

        val lastReported = AtomicInteger(0)
        collectFiles(root, allFiles, skipFolders, isCancelled) { count ->
            // 1% → 20% : 2000 files = 20%
            val p = 1 + ((count * 19) / 2000).coerceAtMost(19)
            if (p != lastReported.get()) {
                lastReported.set(p)
                onProgress(p)
            }
            if (count % 200 == 0) onStatus("Collected $count files…")
        }

        onProgress(20)
        onStatus("Grouping by size…")

        // ============ PHASE 2: Group by size (20% → 25%) ============
        val bySize = mutableMapOf<Long, MutableList<File>>()
        for (f in allFiles) {
            if (isCancelled()) break
            val len = f.length()
            if (len <= 0) continue
            bySize.getOrPut(len) { mutableListOf() }.add(f)
        }
        onProgress(25)

        // ============ PHASE 3: Candidates ============
        val sizeDupGroups: List<List<File>> =
            bySize.values.filter { it.size > 1 }.map { it.toList() }

        if (sizeDupGroups.isEmpty()) {
            onProgress(100)
            onStatus("")
            return@coroutineScope ScanResult(emptyList(), allFiles.size, 0, 0, 0)
        }

        // ============ PHASE 4: Partial hash (25% → 55%) ============
        onStatus("Quick scan…")

        val totalCandidates = sizeDupGroups.sumOf { it.size }.coerceAtLeast(1)
        val partialCounter = AtomicInteger(0)

        val chunks: List<List<List<File>>> =
            sizeDupGroups.chunked((sizeDupGroups.size / 4).coerceAtLeast(1))

        val partialResults: List<Map<String, List<File>>> = chunks.map { chunk ->
            async(Dispatchers.IO) {
                val localMap = HashMap<String, MutableList<File>>()
                for (filesList in chunk) {
                    for (f in filesList) {
                        if (isCancelled()) break
                        val h = partialHash(f) ?: continue
                        localMap.getOrPut(h) { mutableListOf() }.add(f)

                        // Update progress (25 → 55)
                        val done = partialCounter.incrementAndGet()
                        if (done % 3 == 0 || done == totalCandidates) {
                            val p = 25 + ((done * 30) / totalCandidates).coerceAtMost(30)
                            onProgress(p)
                        }
                    }
                }
                localMap as Map<String, List<File>>
            }
        }.awaitAll()

        onProgress(55)
        onStatus("Deep scan…")

        // Merge
        val mergedPartial = HashMap<String, MutableList<File>>()
        for (map in partialResults) {
            for ((k, v) in map) {
                mergedPartial.getOrPut(k) { mutableListOf() }.addAll(v)
            }
        }

        // ============ PHASE 5: Full hash (55% → 100%) ============
        val fullCandidates: List<List<File>> =
            mergedPartial.values.filter { it.size > 1 }.map { it.toList() }

        if (fullCandidates.isEmpty()) {
            onProgress(100)
            onStatus("")
            return@coroutineScope ScanResult(emptyList(), allFiles.size, 0, 0, 0)
        }

        val totalFull = fullCandidates.sumOf { it.size }.coerceAtLeast(1)
        val fullCounter = AtomicInteger(0)

        val fullChunks: List<List<List<File>>> =
            fullCandidates.chunked((fullCandidates.size / 4).coerceAtLeast(1))

        val fullResults: List<List<DupGroup>> = fullChunks.map { chunk ->
            async(Dispatchers.IO) {
                val groups = mutableListOf<DupGroup>()
                for (candidates in chunk) {
                    if (isCancelled()) break
                    val byHash = HashMap<String, MutableList<File>>()
                    for (f in candidates) {
                        val h = fullHash(f) ?: continue
                        byHash.getOrPut(h) { mutableListOf() }.add(f)

                        // Update progress (55 → 100)
                        val done = fullCounter.incrementAndGet()
                        if (done % 2 == 0 || done == totalFull) {
                            val p = 55 + ((done * 45) / totalFull).coerceAtMost(45)
                            onProgress(p)
                        }
                        if (done % 5 == 0) onStatus("Hashing: ${f.name}")
                    }
                    for ((hash, list) in byHash) {
                        if (list.size > 1) {
                            groups.add(
                                DupGroup(
                                    hash = hash,
                                    size = list[0].length(),
                                    files = list.sortedBy { it.lastModified() }
                                )
                            )
                        }
                    }
                }
                groups
            }
        }.awaitAll()

        onProgress(100)
        onStatus("")

        val allGroups: List<DupGroup> = fullResults.flatten()
            .sortedByDescending { it.wastedSpace }

        ScanResult(
            groups = allGroups,
            totalFilesScanned = allFiles.size,
            totalGroups = allGroups.size,
            totalDuplicates = allGroups.sumOf { it.files.size },
            totalWasted = allGroups.sumOf { it.wastedSpace }
        )
    }

    private fun collectFiles(
        dir: File,
        out: MutableList<File>,
        skipFolders: Set<String>,
        isCancelled: () -> Boolean,
        onFileFound: (Int) -> Unit
    ) {
        if (isCancelled()) return
        if (!dir.canRead()) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (isCancelled()) return
            val rel = child.absolutePath.substringAfter("emulated/0/", "")
            if (skipFolders.any { rel.startsWith(it) }) continue
            if (child.isDirectory) {
                if (child.name.startsWith(".")) continue
                collectFiles(child, out, skipFolders, isCancelled, onFileFound)
            } else {
                if (child.name.startsWith(".")) continue
                if (!child.canRead()) continue
                out.add(child)
                onFileFound(out.size)
            }
        }
    }

    private fun partialHash(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buf = ByteArray(4096)
                val n = input.read(buf)
                if (n > 0) md.update(buf, 0, n)
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) { null }
    }

    private fun fullHash(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buf = ByteArray(128 * 1024)
                var n: Int
                while (input.read(buf).also { n = it } > 0) {
                    md.update(buf, 0, n)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) { null }
    }
}