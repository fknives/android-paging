package com.halcyonmobile.android.core.internal.localsource

import com.halcyonmobile.android.core.model.GitHubRepo
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

internal class GitHubRepoLocalSource {

    private val stream = ConflatedBroadcastChannel<List<GitHubRepo>>()

    fun numberOfElementsCached(): Int =
        stream.valueOrNull?.size ?: 0

    fun getFirstElements(numberOfElements: Int): Flow<List<GitHubRepo>> = stream.asFlow().map { it.take(numberOfElements) }

    fun addToCache(toCache: List<GitHubRepo>) {
        stream.offer(stream.valueOrNull.orEmpty() + toCache)
    }

    fun refreshCache(toCache: List<GitHubRepo>) {
        stream.offer(toCache)
    }

    fun clearCache() {
        stream.offer(emptyList())
    }
}