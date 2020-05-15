package com.halcyonmobile.android.paging.repo.log

interface ErrorLogger {
    fun logError(throwable: Throwable)
}