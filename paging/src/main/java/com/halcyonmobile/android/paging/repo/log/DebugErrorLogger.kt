package com.halcyonmobile.android.paging.repo.log

import java.util.logging.Level
import java.util.logging.Logger

class DebugErrorLogger : ErrorLogger {
    private val logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)

    /**
     * Log an error that might occur
     */
    override fun logError(throwable: Throwable) {
        logger.log(Level.SEVERE, if (!throwable.message.isNullOrEmpty()) throwable.message else DEFAULT_ERROR_MESSAGE)
    }

    companion object {
        private const val DEFAULT_ERROR_MESSAGE = "A problem has occurred!"
    }
}