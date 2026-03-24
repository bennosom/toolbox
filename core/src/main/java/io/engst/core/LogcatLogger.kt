package io.engst.core

import android.util.Log

class LogcatLogger(private val tag: String, private val prefix: String) : Logging {
   private fun formatPrefix(vararg name: String): String {
      return buildString {
         name.forEach {
            append('[')
            append(it)
            append("]")
         }
         append(" ")
      }
   }

   override fun logDebug(vararg hints: String, message: () -> Any?) {
      Log.d(tag, formatPrefix(prefix, *hints) + message())
   }

   override fun logInfo(vararg hints: String, message: () -> Any?) {
      Log.i(tag, formatPrefix(prefix, *hints) + message())
   }

   override fun logWarn(vararg hints: String, message: () -> Any?) {
      Log.w(tag, formatPrefix(prefix, *hints) + message())
   }

   override fun logError(error: Throwable?, vararg hints: String, message: () -> Any?) {
      Log.e(tag, formatPrefix(prefix, *hints) + message(), error)
   }
}