package com.halcyonmobile.android.paging.repo

import com.halcyonmobile.android.paging.repo.log.ErrorLogger
import kotlinx.coroutines.flow.Flow

/**
 * Interface that should ease the communication between useCases and the actual repositories that implement it
 * [fetch] - intended for making a call to the remote source
 * [get] - intended for fetching the data that exists locally in the cache
 * [refresh] - intended for making sure that the local data is in sync with the remote
 */
abstract class RepositoryHelper<T>(private val errorLogger: ErrorLogger) {

    abstract suspend fun fetch(numberOfElements: Int): Flow<List<T>>
    abstract suspend fun get(numberOfElements: Int): Flow<List<T>>
    abstract suspend fun refresh(numberOfElements: Int): Flow<List<T>>

    open fun logError(throwable: Throwable) {
        errorLogger.logError(throwable)
    }

    suspend inline fun <T> run(crossinline call: suspend () -> Flow<List<T>>): Flow<List<T>> {
        try {
            return call()
        } catch (throwable: Throwable) {
            logError(throwable)
            throw throwable
        }
    }
}