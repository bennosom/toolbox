package io.engst.core

interface Logging {
   fun logDebug(vararg hints: String, message: () -> Any?)

   fun logInfo(vararg hints: String, message: () -> Any?)

   fun logWarn(vararg hints: String, message: () -> Any?)

   fun logError(error: Throwable? = null, vararg hints: String, message: () -> Any?)
}

private const val TAG = "LauncherApp"

fun scopedLogger(scope: String): Logging = LogcatLogger(TAG, scope)

private val globalLogger = LogcatLogger(TAG, "Global")

fun logDebug(vararg hints: String, message: () -> Any?) =
   globalLogger.logDebug(*hints, message = message)

fun logInfo(vararg hints: String, message: () -> Any?) =
   globalLogger.logInfo(*hints, message = message)

fun logWarn(vararg hints: String, message: () -> Any?) =
   globalLogger.logWarn(*hints, message = message)

fun logError(throwable: Throwable? = null, vararg hints: String, message: () -> Any?) =
   globalLogger.logError(throwable, *hints, message = message)

