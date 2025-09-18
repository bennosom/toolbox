package io.engst.core

import android.util.Log

const val TAG = "App"

fun logDebug(message: () -> String) = Log.d(TAG, message())

fun logInfo(message: () -> String) = Log.i(TAG, message())

fun logWarn(message: () -> String) = Log.w(TAG, message())

fun logError(throwable: Throwable? = null, message: () -> String) = Log.e(TAG, message(), throwable)
