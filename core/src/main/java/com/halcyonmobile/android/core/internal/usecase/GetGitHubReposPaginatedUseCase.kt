package com.halcyonmobile.android.core.internal.usecase

import com.halcyonmobile.android.core.internal.repo.GitHubRepoRepository
import com.halcyonmobile.android.core.model.GitHubRepo
import com.halcyonmobile.android.paging.PagingStateMachine
import kotlinx.coroutines.flow.Flow

class GetGitHubReposPaginatedUseCase internal constructor(private val gitHubRepoRepository: GitHubRepoRepository) {

    operator fun invoke(pageSize: Int = 10) : PagingStateMachine<GitHubRepo> {
        return PagingStateMachine(pageSize = pageSize, requestElements =  { numberOfElements, fetch ->
            wrapRepoResponse {
                if (fetch) gitHubRepoRepository.fetch(numberOfElements) else gitHubRepoRepository.get(numberOfElements)
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