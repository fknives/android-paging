package com.halcyonmobile.android.core

import java.util.logging.Level
import java.util.logging.Logger

class DebugErrorLogger : ErrorLogger {
    private val logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)

    /**
     * Log an error that might occur
     */
    override fun logError(throwable: Throwable) {
        logger.log(Level.SEVERE, if (!throwable.message.isNullOrEmpty()) throwable.message else "A problem has occurred!")
    }
}