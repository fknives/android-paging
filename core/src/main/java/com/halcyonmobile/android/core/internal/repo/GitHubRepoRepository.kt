package com.halcyonmobile.android.core.internal.repo

import com.halcyonmobile.android.core.internal.api.GitHubRepoRemoteSource
import com.halcyonmobile.android.core.internal.localsource.GitHubRepoLocalSource
import com.halcyonmobile.android.core.model.GitHubRepo
import com.halcyonmobile.android.paging.repo.GithubRepoRepositoryHelper
import com.halcyonmobile.android.paging.repo.log.ErrorLogger
import kotlinx.coroutines.flow.Flow

internal class GitHubRepoRepository(
    private val remoteSource: GitHubRepoRemoteSource,
    private val localSource: GitHubRepoLocalSource,
    logger: ErrorLogger
) : GithubRepoRepositoryHelper<GitHubRepo>(logger) {

    /**
     * Makes a call to the [GitHubRepoRemoteSource], caches the fetched data, and returns it from the local storage
     * @param numberOfElements - the number of elements per page
     */
    @Deprecated("The get will handle the initial load as well, and for refreshing we should use the refresh method")
    override suspend fun fetch(numberOfElements: Int): Flow<List<GitHubRepo>> = runWithLogger {
        localSource.clearCache()
        val dataLoaded = remoteSource.getReposPaginated(page = 1, perPage = numberOfElements)
        localSource.addToCache(dataLoaded)
        localSource.getFirstElements(numberOfElements)
    }

    /**
     * Caches the data that is fetched from the remote source to the local cache.
     * @param numberOfElements - the number of elements per page
     */
    override suspend fun refresh(numberOfElements: Int): Flow<List<GitHubRepo>> = runWithLogger {
        localSource.getFirstElements(numberOfElements).also {
            localSource.refreshCache(remoteSource.getReposPaginated(page = 1, perPage = numberOfElements))
        }
    }

    /**
     * Gets the data from the [GitHubRepoLocalSource] with no calls to any remote source and returns it from the local storage
     * @param numberOfElements - the number of elements per page
     */
    override suspend fun get(numberOfElements: Int): Flow<List<GitHubRepo>> = runWithLogger {
        val numberOfElementsCached = localSource.numberOfElementsCached()
        if (numberOfElementsCached < numberOfElements) {
            val dataLoaded = remoteSource.getReposPaginated(
                page = numberOfElements / (numberOfElements - numberOfElementsCached),
                perPage = numberOfElements - numberOfElementsCached
            )
            localSource.addToCache(dataLoaded)
        }
        localSource.getFirstElements(numberOfElements)
    }
}