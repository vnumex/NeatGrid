package com.example.neatgrid.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class LibraryMetadataBackfill(
    private val scope: CoroutineScope,
    private val metadataRepository: MetadataRepository,
    private val improve: suspend (CachedGameMetadata) -> CachedGameMetadata?,
    private val onUpdated: (packageName: String, metadata: GameMetadata) -> Unit
) {
    private val attempted = ConcurrentHashMap.newKeySet<String>()
    private var job: Job? = null

    var enabled: Boolean = false
        set(value) {
            field = value
            if (!value) job?.cancel()
        }

    fun start(candidates: () -> List<String>): Job? {
        if (!enabled) return null
        job?.takeIf { it.isActive }?.let { return it }
        return scope.launch {
            while (enabled) {
                val next = candidates().firstOrNull { attempted.add(it) } ?: break
                backfill(next)
            }
        }.also { job = it }
    }

    private suspend fun backfill(packageName: String) {
        try {
            val cached = readCached(packageName) ?: return
            val improved = improve(cached)?.takeIf { it != cached } ?: return
            if (readCached(packageName) != cached) return
            metadataRepository.save(packageName, improved)
            onUpdated(packageName, improved.metadata)
        } catch (e: CancellationException) {
            attempted.remove(packageName)
            throw e
        } catch (_: Exception) {
        }
    }

    private suspend fun readCached(packageName: String) = withContext(Dispatchers.IO) {
        metadataRepository.readCached(packageName)
    }
}
