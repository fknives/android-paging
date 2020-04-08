package com.halcyonmobile.android.paging

import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Test

// tests that should happen
// default -> InitialLoading
// InitialLoading -> EndReached
// InitialLoading -> Empty
// InitialLoading -> Normal
// InitialLoading -> ErrorInitialLoading
// InitialLoading -> ErrorInitialLoading -> InitialLoading -> EndReached
// InitialLoading -> ErrorInitialLoading -> InitialLoading -> Empty
// InitialLoading -> ErrorInitialLoading -> InitialLoading -> Normal
// InitialLoading -> ErrorInitialLoading -> InitialLoading -> ErrorInitialLoading
// InitialLoading -> Normal  -> LoadingMore -> EndReached
// InitialLoading -> Normal  -> LoadingMore -> Normal
// InitialLoading -> Normal  -> LoadingMore -> ErrorLoadingMore
// InitialLoading -> Normal  -> LoadingMore -> ErrorLoadingMore -> LoadingMore -> EndReached
// InitialLoading -> Normal  -> LoadingMore -> ErrorLoadingMore -> LoadingMore -> Normal
// InitialLoading -> Normal  -> LoadingMore -> ErrorLoadingMore -> LoadingMore -> ErrorLoadingMore
// InitialLoading -> EndReached -> Refresh -> RefreshError
// InitialLoading -> EndReached -> Refresh -> EndReached
// InitialLoading -> EndReached -> Refresh -> Empty
// InitialLoading -> EndReached -> Refresh -> Normal
//todo
// InitialLoading -> Empty -> Refresh -> RefreshError
// InitialLoading -> Empty -> Refresh -> EndReached
// InitialLoading -> Empty -> Refresh -> Empty
// InitialLoading -> Empty -> Refresh -> Normal
// InitialLoading -> Normal -> Refresh -> RefreshError
// InitialLoading -> Normal -> Refresh -> EndReached
// InitialLoading -> Normal -> Refresh -> Empty
// InitialLoading -> Normal -> Refresh -> Normal
// InitialLoading -> LoadingMore -> Refresh -> RefreshError
// InitialLoading -> LoadingMore -> Refresh -> EndReached
// InitialLoading -> LoadingMore -> Refresh -> Empty
// InitialLoading -> ErrorLoadingMore -> Refresh -> Normal
// InitialLoading -> ErrorLoadingMore -> Refresh -> RefreshError
// InitialLoading -> ErrorLoadingMore -> Refresh -> EndReached
// InitialLoading -> ErrorLoadingMore -> Refresh -> Empty
// InitialLoading -> ErrorLoadingMore -> Refresh -> Normal
// InitialLoading -> ErrorInitialLoading -> Refresh == InitialLoading
class PagingStateMachineTest {

    private val testCoroutineDispatcher = TestCoroutineDispatcher()
    private val testCoroutineScope = TestCoroutineScope(testCoroutineDispatcher)

    // region initial loading triggers without error
    // default -> InitialLoading
    @Test
    fun when_openingTheStream_then_loadingIsShown() = testCoroutineScope.runBlockingTest {
        val channel = ConflatedBroadcastChannel<List<String>>()
        val sut = PagingStateMachine<String>(testCoroutineDispatcher, 5) { _, _ -> PagingStateMachine.Answer.Success(channel.asFlow()) }

        val actual = sut.pagedDataStream.take(1).toList()

        Assert.assertEquals(listOf(PagedResult(emptyList<String>(), PagedState.LoadingInitial)), actual)
    }
    // InitialLoading -> EndReached
    @Test
    fun when_openingTheStreamAndReturningDataLessThanPageSize_then_loadingIsShownThenDataIsShownInEndState() = testCoroutineScope.runBlockingTest {
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,5) { _, _ -> PagingStateMachine.Answer.Success(flowOf(listOf("alma"))) }

        val actual = sut.pagedDataStream.take(2).toList()

        Assert.assertEquals(listOf(PagedResult(emptyList<String>(), PagedState.LoadingInitial), PagedResult(listOf("alma"), PagedState.EndReached)), actual)
    }

    // InitialLoading -> Empty
    @Test
    fun when_openingTheStreamAndReturningEmptyData_then_loadingIsShownThenDataIsShownInEmptyState() = testCoroutineScope.runBlockingTest {
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,5) { _, _ -> PagingStateMachine.Answer.Success(flowOf(listOf<String>())) }

        val actual = sut.pagedDataStream.take(2).toList()

        Assert.assertEquals(listOf(PagedResult(emptyList<String>(), PagedState.LoadingInitial), PagedResult(listOf(), PagedState.EmptyState)), actual)
    }

    // InitialLoading -> Normal
    @Test
    fun when_openingTheStreamAndReturningData_then_loadingIsShownThenDataIsShownInNormalState() = testCoroutineScope.runBlockingTest {
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,3) { _, _ -> PagingStateMachine.Answer.Success(flowOf(listOf("alma", "banan", "citrom"))) }

        val actual = sut.pagedDataStream.take(2).toList()

        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual[0])
        Assert.assertThat(actual[1], createPagedResultMatcher("Paged result is not Normal Or the data is not matching", createNormalValidate(listOf("alma", "banan", "citrom"))))
    }
    // endregion

    // InitialLoading -> ErrorInitialLoading
    @Test
    fun when_openingTheStreamAndReturningError_then_loadingIsShownThenErrorInitialLoading() = testCoroutineScope.runBlockingTest {
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,5) { _, _ -> PagingStateMachine.Answer.Failure<String>(Throwable()) }

        val actual = sut.pagedDataStream.take(2).toList()

        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual.first())
        Assert.assertThat(actual[1], createPagedResultMatcher("Paged result is not ErrorLoadingInitial Or the data is not empty", ::errorLoadingInitialValidate))
    }

    // region after initial error
    // InitialLoading -> ErrorInitialLoading -> InitialLoading -> ErrorInitialLoading
    @Test
    fun when_openingTheStreamAndFirstReturningErrorTheErrorAgain_then_loadingIsShownThenErrorInitialLoadingThenLoadingThenInitialError() = runBlocking {
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,3) { _, _ ->
                PagingStateMachine.Answer.Failure<String>(Throwable())
        }

        val actual = sut.pagedDataStream.take(4)
            .doSideEffect { (it.pagedState as? PagedState.ErrorLoadingInitial)?.run { sut.retryLoadingInitial() } }
            .toList()

        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual.first())
        Assert.assertThat(actual[1], createPagedResultMatcher("Paged result is not ErrorLoadingInitial Or the data is not empty", ::errorLoadingInitialValidate))
        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual[2])
        Assert.assertThat(actual[3], createPagedResultMatcher("Paged result is not ErrorLoadingInitial Or the data is not empty", ::errorLoadingInitialValidate))
    }

    // InitialLoading -> ErrorInitialLoading -> InitialLoading -> Normal
    @Test
    fun when_openingTheStreamAndFirstReturningErrorThePageSizeData_then_loadingIsShownThenErrorInitialLoadingThenLoadingThenNormal() = runBlocking {
        var isFirst: Boolean = false
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,3) { _, _ ->
            if (!isFirst) {
                isFirst = true
                PagingStateMachine.Answer.Failure<String>(Throwable())
            } else {
                PagingStateMachine.Answer.Success<String>(flowOf(listOf("alma", "banan", "citrom")))
            }
        }

        val actual = sut.pagedDataStream.take(4)
            .doSideEffect { (it.pagedState as? PagedState.ErrorLoadingInitial)?.run { sut.retryLoadingInitial() } }
            .toList()

        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual.first())
        Assert.assertThat(actual[1], createPagedResultMatcher("Paged result is not ErrorLoadingInitial Or the data is not empty", ::errorLoadingInitialValidate))
        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual[2])
        Assert.assertThat(actual[3], createPagedResultMatcher("Paged result is not Normal Or the data is not matching", createNormalValidate(listOf("alma", "banan", "citrom"))))
    }

    // InitialLoading -> ErrorInitialLoading -> InitialLoading -> EndReached
    @Test
    fun when_openingTheStreamAndFirstReturningErrorTheLessThanPageSizeData_then_loadingIsShownThenErrorInitialLoadingThenLoadingThenEndReached() = runBlocking {
        var isFirst: Boolean = false
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,3) { _, _ ->
            if (!isFirst) {
                isFirst = true
                PagingStateMachine.Answer.Failure<String>(Throwable())
            } else {
                PagingStateMachine.Answer.Success<String>(flowOf(listOf("alma")))
            }
        }

        val actual = sut.pagedDataStream.take(4)
            .doSideEffect { (it.pagedState as? PagedState.ErrorLoadingInitial)?.run { sut.retryLoadingInitial() } }
            .toList()

        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual.first())
        Assert.assertThat(actual[1], createPagedResultMatcher("Paged result is not ErrorLoadingInitial Or the data is not empty", ::errorLoadingInitialValidate))
        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual[2])
        Assert.assertEquals(PagedResult<String>(listOf("alma"), PagedState.EndReached), actual[3])
    }

    // InitialLoading -> ErrorInitialLoading -> InitialLoading -> Empty
    @Test
    fun when_openingTheStreamAndFirstReturningErrorTheEmpty_then_loadingIsShownThenErrorInitialLoadingThenLoadingThenEmpty() = runBlocking {
        var isFirst: Boolean = false
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,3) { _, _ ->
            if (!isFirst) {
                isFirst = true
                PagingStateMachine.Answer.Failure<String>(Throwable())
            } else {
                PagingStateMachine.Answer.Success<String>(flowOf(listOf()))
            }
        }

        val actual = sut.pagedDataStream.take(4)
            .doSideEffect { (it.pagedState as? PagedState.ErrorLoadingInitial)?.run { sut.retryLoadingInitial() } }
            .toList()

        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual.first())
        Assert.assertThat(actual[1], createPagedResultMatcher("Paged result is not ErrorLoadingInitial Or the data is not empty", ::errorLoadingInitialValidate))
        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual[2])
        Assert.assertEquals(PagedResult<String>(listOf(), PagedState.EmptyState), actual[3])
    }
    // endregion

    // todo why it isn't completing with runBlocking test
    // InitialLoading -> Normal  -> LoadingMore -> Normal
    @Test
    fun when_openingTheStreamAndReturningDataThenTriggerLoadingMoreAndReturnMoreData_then_loadingIsShownThenNormalThenLoadingMoreThenNormal() = runBlocking {
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,3) { pageSize, _ ->
            PagingStateMachine.Answer.Success(flowOf(listOf("alma", "banan", "citrom", "dinnye", "eper", "fank?").take(pageSize)))
        }

        val actual = sut.pagedDataStream.take(4)
            .doSideEffect { (it.pagedState as? PagedState.Normal)?.run { sut.onDataBound(it.data.size - 1) } }
            .toList()

        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual[0])
        Assert.assertThat(actual[1], createPagedResultMatcher("Paged result is not Normal Or the data is not matching", createNormalValidate(listOf("alma", "banan", "citrom"))))
        Assert.assertEquals(PagedResult(listOf<String>("alma","banan","citrom"), PagedState.LoadingMore), actual[2])
        Assert.assertThat(actual[3], createPagedResultMatcher("Paged result is not Normal Or the data is not matching", createNormalValidate(listOf("alma", "banan", "citrom","dinnye","eper","fank?"))))
    }

    // InitialLoading -> Normal  -> LoadingMore -> EndReached
    @Test
    fun when_openingTheStreamAndReturningDataThenTriggerLoadingMoreAndReturnSameData_then_loadingIsShownThenNormalThenLoadingMoreThenEnd() = runBlocking {
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,3) { pageSize, _ ->
            PagingStateMachine.Answer.Success(flowOf(listOf("alma", "banan", "citrom").take(pageSize)))
        }

        val actual = sut.pagedDataStream.take(4)
            .doSideEffect { (it.pagedState as? PagedState.Normal)?.run { sut.onDataBound(it.data.size - 1) } }
            .toList()

        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual[0])
        Assert.assertThat(actual[1], createPagedResultMatcher("Paged result is not Normal Or the data is not matching", createNormalValidate(listOf("alma", "banan", "citrom"))))
        Assert.assertEquals(PagedResult(listOf<String>("alma","banan","citrom"), PagedState.LoadingMore), actual[2])
        Assert.assertEquals(PagedResult(listOf("alma","banan","citrom"), PagedState.EndReached), actual[3])
    }

    // InitialLoading -> Normal  -> LoadingMore -> ErrorLoadingMore
    @Test
    fun when_openingTheStreamAndReturningDataThenTriggerLoadingMoreAndError_then_loadingIsShownThenNormalThenLoadingMoreThenLoadingError() = runBlocking {
        var isFirst = true
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,3) { pageSize, _ ->
            if (isFirst) {
                isFirst = false
                PagingStateMachine.Answer.Success(flowOf(listOf("alma", "banan", "citrom").take(pageSize)))
            } else {
                PagingStateMachine.Answer.Failure(Throwable())
            }
        }

        val actual = sut.pagedDataStream.take(4)
            .doSideEffect { (it.pagedState as? PagedState.Normal)?.run { sut.onDataBound(it.data.size - 1) } }
            .toList()

        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual[0])
        Assert.assertThat(actual[1], createPagedResultMatcher("Paged result is not Normal Or the data is not matching", createNormalValidate(listOf("alma", "banan", "citrom"))))
        Assert.assertEquals(PagedResult(listOf<String>("alma","banan","citrom"), PagedState.LoadingMore), actual[2])
        Assert.assertThat(actual[3], createPagedResultMatcher("Paged result is not ErrorLoadingMore Or the data is not matching", createErrorLoadingMoreValidate(listOf("alma", "banan", "citrom"))))
    }

    // InitialLoading -> Normal  -> LoadingMore -> ErrorLoadingMore -> LoadingMore -> Normal
    @Test
    fun when_openingTheStreamAndReturningDataThenTriggerLoadingMoreAndErrorThenRetryAndNormal_then_loadingIsShownThenNormalThenLoadingMoreThenLoadingErrorThenLoadingMoreThenData() = runBlocking {
        var requestIndex = 0
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,3) { pageSize, _ ->
            requestIndex++
            if (requestIndex == 1 || requestIndex == 3) {
                PagingStateMachine.Answer.Success(flowOf(listOf("alma", "banan", "citrom","dinnye","eper","fank?").take(pageSize)))
            } else {
                PagingStateMachine.Answer.Failure(Throwable())
            }
        }

        val actual = sut.pagedDataStream.take(6)
            .doSideEffect { (it.pagedState as? PagedState.Normal)?.run { sut.onDataBound(it.data.size -1) } }
            .doSideEffect { (it.pagedState as? PagedState.ErrorLoadingMore)?.run { sut.retryLoadingMore() } }
            .toList()

        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual[0])
        Assert.assertThat(actual[1], createPagedResultMatcher("Paged result is not Normal Or the data is not matching", createNormalValidate(listOf("alma", "banan", "citrom"))))
        Assert.assertEquals(PagedResult(listOf<String>("alma","banan","citrom"), PagedState.LoadingMore), actual[2])
        Assert.assertThat(actual[3], createPagedResultMatcher("Paged result is not ErrorLoadingMore Or the data is not matching", createErrorLoadingMoreValidate(listOf("alma", "banan", "citrom"))))
        Assert.assertEquals(PagedResult(listOf<String>("alma","banan","citrom"), PagedState.LoadingMore), actual[4])
        Assert.assertThat(actual[5], createPagedResultMatcher("Paged result is not Normal Or the data is not matching", createNormalValidate(listOf("alma", "banan", "citrom","dinnye","eper","fank?"))))
    }

    // InitialLoading -> Normal  -> LoadingMore -> ErrorLoadingMore -> LoadingMore -> EndReached
    @Test
    fun when_openingTheStreamAndReturningDataThenTriggerLoadingMoreAndErrorThenRetryAndLessThanPage_then_loadingIsShownThenNormalThenLoadingMoreThenLoadingErrorThenLoadingMoreThenEnd() = runBlocking {
        var requestIndex = 0
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,3) { pageSize, _ ->
            requestIndex++
            if (requestIndex == 1 || requestIndex == 3) {
                PagingStateMachine.Answer.Success(flowOf(listOf("alma", "banan", "citrom","dinnye","eper").take(pageSize)))
            } else {
                PagingStateMachine.Answer.Failure(Throwable())
            }
        }

        val actual = sut.pagedDataStream.take(6)
            .doSideEffect { (it.pagedState as? PagedState.Normal)?.run { sut.onDataBound(it.data.size -1) } }
            .doSideEffect { (it.pagedState as? PagedState.ErrorLoadingMore)?.run { sut.retryLoadingMore() } }
            .toList()

        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual[0])
        Assert.assertThat(actual[1], createPagedResultMatcher("Paged result is not Normal Or the data is not matching", createNormalValidate(listOf("alma", "banan", "citrom"))))
        Assert.assertEquals(PagedResult(listOf<String>("alma","banan","citrom"), PagedState.LoadingMore), actual[2])
        Assert.assertThat(actual[3], createPagedResultMatcher("Paged result is not ErrorLoadingMore Or the data is not matching", createErrorLoadingMoreValidate(listOf("alma", "banan", "citrom"))))
        Assert.assertEquals(PagedResult(listOf<String>("alma","banan","citrom"), PagedState.LoadingMore), actual[4])
        Assert.assertEquals(PagedResult(listOf("alma", "banan", "citrom","dinnye","eper"), PagedState.EndReached), actual[5])
    }

    // InitialLoading -> Normal  -> LoadingMore -> ErrorLoadingMore -> LoadingMore -> ErrorLoadingMore
    @Test
    fun when_openingTheStreamAndReturningDataThenTriggerLoadingMoreAndErrorThenRetryAndErrorAgain_then_loadingIsShownThenNormalThenLoadingMoreThenLoadingErrorThenLoadingMoreThenErrorLoadingMore() = runBlocking {
        var requestIndex = 0
        val sut = PagingStateMachine<String>(testCoroutineDispatcher,3) { pageSize, _ ->
            requestIndex++
            if (requestIndex == 1) {
                PagingStateMachine.Answer.Success(flowOf(listOf("alma", "banan", "citrom", "dinnye", "eper", "fank?").take(pageSize)))
            } else {
                PagingStateMachine.Answer.Failure(Throwable())
            }
        }

        val actual = sut.pagedDataStream.take(6)
            .doSideEffect { (it.pagedState as? PagedState.Normal)?.run { sut.onDataBound(it.data.size -1) } }
            .doSideEffect { (it.pagedState as? PagedState.ErrorLoadingMore)?.run { sut.retryLoadingMore() } }
            .toList()

        Assert.assertEquals(PagedResult(emptyList<String>(), PagedState.LoadingInitial), actual[0])
        Assert.assertThat(actual[1], createPagedResultMatcher("Paged result is not Normal Or the data is not matching", createNormalValidate(listOf("alma", "banan", "citrom"))))
        Assert.assertEquals(PagedResult(listOf<String>("alma", "banan", "citrom"), PagedState.LoadingMore), actual[2])
        Assert.assertThat(actual[3], createPagedResultMatcher("Paged result is not ErrorLoadingMore Or the data is not matching", createErrorLoadingMoreValidate(listOf("alma", "banan", "citrom"))))
        Assert.assertEquals(PagedResult(listOf<String>("alma", "banan", "citrom"), PagedState.LoadingMore), actual[4])
        Assert.assertThat(actual[3], createPagedResultMatcher("Paged result is not ErrorLoadingMore Or the data is not matching", createErrorLoadingMoreValidate(listOf("alma", "banan", "citrom"))))
    }

        companion object {

        private fun <T> Flow<T>.doSideEffect(sideEffect: (T) -> Unit): Flow<T> = map {
            sideEffect(it)

            it
        }

        private fun <T> createErrorLoadingMoreValidate(expected: List<T>) = { pagedResult: PagedResult<T> ->
            pagedResult.data == expected && pagedResult.pagedState is PagedState.ErrorLoadingMore
        }

        private fun <T> createNormalValidate(expected: List<T>) = { pagedResult: PagedResult<T> ->
            pagedResult.data == expected && pagedResult.pagedState is PagedState.Normal
        }

        private fun <T> errorLoadingInitialValidate(pagedResult: PagedResult<T>) =
            pagedResult.data.isEmpty() && pagedResult.pagedState is PagedState.ErrorLoadingInitial

        private fun <T> createPagedResultMatcher(errorText: String, validate: (PagedResult<T>) -> Boolean): Matcher<PagedResult<T>> = object : BaseMatcher<PagedResult<T>>() {

            override fun describeTo(description: Description?) {

            }

            override fun describeMismatch(item: Any?, mismatchDescription: Description?) {
                mismatchDescription?.appendText("$errorText. actual value: $item")
            }

            override fun matches(item: Any?): Boolean = item is PagedResult<*> && validate(item as PagedResult<T>)
        }
    }
}