package com.halcyonmobile.android.core.internal.usecase

import com.halcyonmobile.android.core.ErrorLogger
import com.halcyonmobile.android.core.internal.repo.GithubRepoRepositoryHelper
import com.halcyonmobile.android.core.model.GitHubRepo
import com.halcyonmobile.android.paging.PagingStateMachine
import kotlinx.coroutines.flow.Flow

class GetGitHubReposPaginatedUseCase internal constructor(private val gitHubRepoGithubRepoRepository: GithubRepoRepositoryHelper<GitHubRepo>) {

    operator fun invoke(pageSize: Int = 10): PagingStateMachine<GitHubRepo> {
        return PagingStateMachine(pageSize = pageSize, requestElements = { numberOfElements, refresh ->
            wrapRepoResponse(gitHubRepoGithubRepoRepository.errorLogger) {
                if (refresh) gitHubRepoRepository.refresh(numberOfElements) else gitHubRepoRepository.get(numberOfElements)
            }
        })
    }

    companion object {
        private suspend inline fun <T> wrapRepoResponse(logger: ErrorLogger, crossinline call: suspend () -> Flow<List<T>>): PagingStateMachine.Answer<T> {
            val data = try {
                call()
            } catch (throwable: Throwable) {
                logger.logError(throwable)
                return PagingStateMachine.Answer.Failure(throwable)
            }
            return PagingStateMachine.Answer.Success(data)
        }
    }
}