package com.halcyonmobile.android.paging

import com.halcyonmobile.android.paging.internal.InternalState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.coroutineContext

class PagingStateMachine<T>(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val pageSize: Int,
    private val requestElements: suspend (numberOfElements: Int, isFetch: Boolean) -> Answer<T>
) {

    private var cancelCurrentRequest: Job = Job()
    private val stream = ConflatedBroadcastChannel<InternalState<T>>()

    val pagedDataStream: Flow<PagedResult<T>> = stream
        .apply { offer(InternalState.ShowLoadingInitial(flowOf(emptyList()), 0, false, false)) } // todo refreshing
        .asFlow()
        .flowOn(dispatcher)
        .onEach { onInternalState(it) }
        .mapNotNull { internalStateToDataWithPagedState(it) }
        .flatMapLatest { it }

    fun fetch() {
        cancelCurrentRequest.cancel()
        cancelCurrentRequest = Job()
        stream.offer(InternalState.ShowLoadingInitial(flowOf(emptyList()), 0, true, false))
    }

    fun onDataBound(position: Int) {
        stream.valueOrNull?.takeUnless { it is InternalState.DoLoading || it is InternalState.DataAtPositionBound }?.let {
            if (it.numberOfElements - 5 < position) {
                stream.offer(InternalState.DataAtPositionBound(it.dataStream, it.numberOfElements))
            }
        }
    }

    fun retryLoadingInitial() {
        (stream.valueOrNull as? InternalState.InitialLoadingFailed)?.let {
            stream.offer(InternalState.ShowLoadingInitial(it.dataStream, it.numberOfElements, it.fetch, false))
        }
    }

    fun retryLoadingMore() {
        (stream.valueOrNull as? InternalState.LoadingMoreFailed)?.let {
            stream.offer(InternalState.ShowLoadingMore(it.dataStream, it.numberOfElements))
        }
    }

    private suspend fun internalStateToDataWithPagedState(internalState: InternalState<T>): Flow<PagedResult<T>>? =
        when (internalState) {
            is InternalState.ShowLoadingInitial -> showLoading(internalState)
            is InternalState.DoLoading -> null
            is InternalState.ShowLoadingMore -> showLoadingMore(internalState)
            is InternalState.InitialLoadingFailed -> showInitialLoadingFailed(internalState)
            is InternalState.DataLoaded -> showDataLoaded(internalState)
            is InternalState.DataAtPositionBound -> null
            is InternalState.LoadingMoreFailed -> showLoadingMoreFailed(internalState)
        }

    private fun showLoadingMore(internalState: InternalState.ShowLoadingMore<T>): Flow<PagedResult<T>> =
        combineFlowWithState(internalState.dataStream, PagedState.LoadingMore)

    private fun showDataLoaded(internalState: InternalState.DataLoaded<T>): Flow<PagedResult<T>> {
        return internalState.dataStream.map { data ->
            val pagedState = when {
                internalState.isInitial && data.isEmpty() -> PagedState.EmptyState
                data.size < internalState.numberOfElements -> PagedState.EndReached
                else -> PagedState.Normal
            }
            PagedResult(data, pagedState)
        }
    }

    private fun showInitialLoadingFailed(internalState: InternalState.InitialLoadingFailed<T>): Flow<PagedResult<T>> =
        combineFlowWithState(internalState.dataStream, PagedState.ErrorLoadingInitial(internalState.cause))

    private fun showLoadingMoreFailed(internalState: InternalState.LoadingMoreFailed<T>): Flow<PagedResult<T>> =
        combineFlowWithState(internalState.dataStream, PagedState.ErrorLoadingMore(internalState.cause))

    private suspend fun onInternalState(internalState: InternalState<T>) =
        when (internalState) {
            is InternalState.ShowLoadingInitial -> onShowInitialLoading(internalState)
            is InternalState.DoLoading -> onDoLoading(internalState)
            is InternalState.ShowLoadingMore -> onShowLoadingMore(internalState)
            is InternalState.InitialLoadingFailed -> Unit
            is InternalState.DataAtPositionBound -> onDataAtPositionBound(internalState)
            is InternalState.DataLoaded -> Unit
            is InternalState.LoadingMoreFailed -> Unit
        }

    private fun onShowLoadingMore(internalState: InternalState.ShowLoadingMore<T>) {
        stream.offer(InternalState.DoLoading(internalState.dataStream, internalState.numberOfElements, false, false, false))
    }

    private fun onDataAtPositionBound(internalState: InternalState.DataAtPositionBound<T>) {
        stream.offer(InternalState.ShowLoadingMore(internalState.dataStream, internalState.numberOfElements))
    }

    private fun onShowInitialLoading(internalState: InternalState.ShowLoadingInitial<T>) {
        stream.offer(InternalState.DoLoading(internalState.dataStream, internalState.numberOfElements, true, internalState.fetch, false))
    }

    private fun showLoading(internalState: InternalState.ShowLoadingInitial<T>): Flow<PagedResult<T>> =
        combineFlowWithState(internalState.dataStream, PagedState.LoadingInitial)

    private suspend fun onDoLoading(internalState: InternalState.DoLoading<T>) {
        // todo
        val nextInternalState = CoroutineScope(coroutineContext + cancelCurrentRequest).async {
            val answer = requestElements(internalState.numberOfElements + pageSize, internalState.fetch)

            when {
                answer is Answer.Success -> {
                    InternalState.DataLoaded(
                        internalState,
                        internalState.numberOfElements + pageSize,
                        answer.data
                    )
                }
                answer is Answer.Failure && internalState.isInitial ->
                    InternalState.InitialLoadingFailed(internalState.dataStream, internalState.numberOfElements, internalState.fetch, false, answer.cause)

                answer is Answer.Failure -> InternalState.LoadingMoreFailed(internalState.dataStream, internalState.numberOfElements, answer.cause)
                else -> throw IllegalStateException("Somehow answer was neither Success nor Failure, that should never happen.")
            }
        }.await()

        stream.offer(nextInternalState)
    }

    private fun combineFlowWithState(flow: Flow<List<T>>, state: PagedState): Flow<PagedResult<T>> =
        flow.map { PagedResult(it, state) }

    sealed class Answer<T> {
        data class Success<T>(val data: Flow<List<T>>) : Answer<T>()
        data class Failure<T>(val cause: Throwable) : Answer<T>()
    }

}