package io.bouckaert.quicksa1.utility.ingestor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T> Flow<T>.chunked(chunkSize: Int): Flow<List<T>> {
    val buffer = mutableListOf<T>()
    return flow {
        this@chunked.collect {
            buffer.add(it)
            if (buffer.size == chunkSize) {
                emit(buffer.toList())
                buffer.clear()
            }
        }
        if (buffer.isNotEmpty()) {
            emit(buffer.toList())
        }
    }
}

fun <K, V> Map<K, V>.chunked(chunkSize: Int): List<Map<K, V>> =
    this.entries.chunked(chunkSize).map { it.associate { (k, v) -> k to v } }