package com.halcyonmobile.android.core

interface ErrorLogger {
    fun logError(throwable: Throwable)
}