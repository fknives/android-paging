package com.halcyonmobile.android.core.internal.usecase

import com.halcyonmobile.android.core.model.GitHubRepo
import com.halcyonmobile.android.paging.PagingStateMachine
import com.halcyonmobile.android.paging.repo.GithubRepoRepositoryHelper
import kotlinx.coroutines.flow.Flow

class GetGitHubReposPaginatedUseCase internal constructor(private val gitHubRepoRepository: GithubRepoRepositoryHelper<GitHubRepo>) {

    operator fun invoke(pageSize: Int = 10): PagingStateMachine<GitHubRepo> {
        return PagingStateMachine(pageSize = pageSize, requestElements = { numberOfElements, refresh ->
            wrapRepoResponse {
                if (refresh) gitHubRepoRepository.refresh(numberOfElements) else gitHubRepoRepository.get(numberOfElements)
            }
        })
    }

    companion object {
        private suspend inline fun <T> wrapRepoResponse(crossinline call: suspend () -> Flow<List<T>>): PagingStateMachine.Answer<T> {
            val data = try {
                call()
            } catch (throwable: Throwable) {
                return PagingStateMachine.Answer.Failure(throwable)
            }
            return PagingStateMachine.Answer.Success(data)
        }
    }
}