package com.halcyonmobile.android.core.internal.repo

import com.halcyonmobile.android.core.internal.api.GitHubRepoRemoteSource
import com.halcyonmobile.android.core.internal.localsource.GitHubRepoLocalSource
import com.halcyonmobile.android.core.model.GitHubRepo
import kotlinx.coroutines.flow.Flow

internal class GitHubRepoRepository(
    private val remoteSource: GitHubRepoRemoteSource,
    private val localSource: GitHubRepoLocalSource
) {

    @Deprecated("The get will handle the initial load as well, and for refreshing we should use the refresh method")
    suspend fun fetch(numberOfElements: Int): Flow<List<GitHubRepo>> {
        localSource.clearCache()
        val dataLoaded = remoteSource.getReposPaginated(page = 1, perPage = numberOfElements)
        localSource.addToCache(dataLoaded)

        return localSource.getFirstElements(numberOfElements)
    }

    suspend fun refresh(numberOfElements: Int): Flow<List<GitHubRepo>> = localSource.getFirstElements(numberOfElements).also {
        localSource.refreshCache(remoteSource.getReposPaginated(page = 1, perPage = numberOfElements))
    }

    suspend fun get(numberOfElements: Int): Flow<List<GitHubRepo>> {
        val numberOfElementsCached = localSource.numberOfElementsCached()
        if (numberOfElementsCached < numberOfElements) {
            val dataLoaded = remoteSource.getReposPaginated(
                page = numberOfElements / (numberOfElements - numberOfElementsCached),
                perPage = numberOfElements - numberOfElementsCached
            )
            localSource.addToCache(dataLoaded)
        }

        return localSource.getFirstElements(numberOfElements)
    }
}