package com.halcyonmobile.android.paging

sealed class PagedState {
    object EmptyState : PagedState()
    object LoadingMore : PagedState()
    object LoadingInitial: PagedState()
    class ErrorLoadingInitial(val cause: Throwable) : PagedState()
    class ErrorLoadingMore(val cause: Throwable) : PagedState()
    object Normal : PagedState()
    object Refreshing: PagedState()
    class RefreshingError(val cause: Throwable) : PagedState()
    object EndReached: PagedState()
}