package com.app.ujjivanbankscrapper.Utils

import android.os.Handler
import android.os.Looper
import android.util.Log

class AutoRunner(callback: Runnable) {
    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable: Runnable

    init {
        checkRunnable = Runnable {
            Log.d("Ticker", "Ticker is idle")
            callback.run()
        }
    }

    fun startRunning() {
        handler.postDelayed(checkRunnable, CHECK_INTERVAL)
    }

    private fun removeCurrentRunning() {
        handler.removeCallbacks(checkRunnable)
    }

    fun startReAgain() {
        removeCurrentRunning()
        handler.postDelayed(checkRunnable, CHECK_INTERVAL)
    }

    companion object {
        private const val CHECK_INTERVAL: Long = 4000
    }
}
