package com.halcyonmobile.android.core.internal.repo

import com.halcyonmobile.android.core.ErrorLogger
import com.halcyonmobile.android.core.internal.api.GitHubRepoRemoteSource
import com.halcyonmobile.android.core.internal.localsource.GitHubRepoLocalSource
import com.halcyonmobile.android.core.model.GitHubRepo
import kotlinx.coroutines.flow.Flow

internal class GitHubRepoGithubRepoRepository(
    private val remoteSource: GitHubRepoRemoteSource,
    private val localSource: GitHubRepoLocalSource,
    logger: ErrorLogger
) : GithubRepoRepositoryHelper<GitHubRepo>(logger) {

    /**
     * Makes a call to the [GitHubRepoRemoteSource], caches the fetched data, and returns it from the local storage
     * @param numberOfElements - the number of elements per page
     */
    override suspend fun fetch(numberOfElements: Int): Flow<List<GitHubRepo>> {
        localSource.clearCache()
        val loadedData = remoteSource.getReposPaginated(page = 1, perPage = numberOfElements)
        localSource.addToCache(loadedData)

        return localSource.getFirstElements(numberOfElements)
    }

    suspend fun refresh(numberOfElements: Int): Flow<List<GitHubRepo>> {
        return fetch(numberOfElements)
    }

    /**
     * Gets the data from the [GitHubRepoLocalSource] with no calls to any remote source and returns it from the local storage
     * @param numberOfElements - the number of elements per page
     */
    override suspend fun get(numberOfElements: Int): Flow<List<GitHubRepo>> {
        val numberOfElementsCached = localSource.numberOfElementsCached()
        if (numberOfElementsCached < numberOfElements) {
            val loadedData = remoteSource.getReposPaginated(
                page = numberOfElements / (numberOfElements - numberOfElementsCached),
                perPage = numberOfElements - numberOfElementsCached
            )
            localSource.addToCache(loadedData)
        }

        return localSource.getFirstElements(numberOfElements)
    }
}