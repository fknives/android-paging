package com.halcyonmobile.android.core.internal.repo

import com.halcyonmobile.android.core.internal.api.GitHubRepoRemoteSource
import com.halcyonmobile.android.core.internal.localsource.GitHubRepoLocalSource
import com.halcyonmobile.android.core.model.GitHubRepo
import com.halcyonmobile.android.paging.repo.RepositoryHelper
import com.halcyonmobile.android.paging.repo.log.ErrorLogger
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow

internal class GitHubRepoRepository(
    private val remoteSource: GitHubRepoRemoteSource,
    private val localSource: GitHubRepoLocalSource,
    logger: ErrorLogger
) : RepositoryHelper<GitHubRepo>(logger) {

    override val isEndReached = ConflatedBroadcastChannel<Boolean>()

    /**
     * Caches the data that is fetched from the remote source to the local cache.
     * @param numberOfElements - the number of elements per page
     */
    override suspend fun refresh(numberOfElements: Int): Flow<List<GitHubRepo>> = run {
        localSource.getFirstElements(numberOfElements).also {
            localSource.refreshCache(remoteSource.getReposPaginated(page = 1, perPage = numberOfElements))
        }
    }

    /**
     * Gets the data from the [GitHubRepoLocalSource] with no calls to any remote source and returns it from the local storage
     * @param numberOfElements - the number of elements per page
     */
    override suspend fun get(numberOfElements: Int): Flow<List<GitHubRepo>> = run {
        val numberOfElementsCached = localSource.numberOfElementsCached()
        if (numberOfElementsCached < numberOfElements) {
            val perPage = numberOfElements - numberOfElementsCached
            val dataLoaded = remoteSource.getReposPaginated(
                page = numberOfElements / (numberOfElements - numberOfElementsCached),
                perPage = perPage
            )
            localSource.addToCache(dataLoaded)
            if (dataLoaded.size < perPage) {
                isEndReached.send(true)
            }
        }
        localSource.getFirstElements(numberOfElements)
    }
}