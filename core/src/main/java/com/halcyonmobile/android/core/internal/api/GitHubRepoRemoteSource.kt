package com.halcyonmobile.android.core.internal.api

import com.halcyonmobile.android.core.model.GitHubRepo

internal class GitHubRepoRemoteSource(private val githubRepoService: GitHubService) {

    suspend fun getReposPaginated(page: Int, perPage: Int): List<GitHubRepo> =
        try {
            githubRepoService.getReposPaginated(page, perPage).map {
                GitHubRepo(
                    nodeId = it.nodeId,
                    name = it.name,
                    description = it.description.orEmpty(),
                    htmlUrl = it.htmlUrl.orEmpty(),
                    isPrivate = it.isPrivate,
                    numberOfWatchers = it.numberOfWatchers ?: 0
                )
            }
        } catch (throwable: Throwable){
            throwable.printStackTrace()
            throw throwable
        }
}