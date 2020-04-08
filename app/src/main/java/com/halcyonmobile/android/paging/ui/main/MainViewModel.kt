package com.halcyonmobile.android.paging.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import com.halcyonmobile.android.core.internal.usecase.GetGitHubReposPaginatedUseCase
import kotlinx.coroutines.flow.collect

// todo proper error logging
class MainViewModel(getGitHubReposPaginated: GetGitHubReposPaginatedUseCase) : ViewModel() {

    private val paginatedDataInteractor = getGitHubReposPaginated(20)
    private val result = liveData {
        try {
            paginatedDataInteractor.pagedDataStream.collect { emit(it) }
        } catch (throwable: Throwable){
            throwable.printStackTrace()
        }
    }
    val dataStream = result.map { it.data }
    val state = result.map { it.pagedState }

    fun onRefresh() {
        paginatedDataInteractor.fetch()
    }

    fun onRetryLoadingInitial() {
        paginatedDataInteractor.retryLoadingInitial()
    }

    fun onRetryLoadingMore() {
        paginatedDataInteractor.retryLoadingMore()
    }

    // todo this will be moved into a "smartlist"
    fun onDataAtPositionBound(position: Int) {
        paginatedDataInteractor.onDataBound(position)
    }
}
