package com.halcyonmobile.android.paging

data class PagedResult<T>(val data: List<T>, val pagedState: PagedState)