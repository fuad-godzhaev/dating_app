package com.apiguave.pds

import android.content.Context

/**
 * Singleton manager for PDS operations.
 *
 * Provides a simple interface to start, stop, and check PDS status.
 */
object PdsManager {

    private const val PREFS_NAME = "pds_prefs"
    private const val KEY_AUTO_START = "auto_start_pds"

    /**
     * Starts PDS as a background service.
     */
    fun start(context: Context) {
        PdsBackgroundService.start(context.applicationContext)
    }

    /**
     * Stops PDS background service.
     */
    fun stop(context: Context) {
        PdsBackgroundService.stop(context.applicationContext)
    }

    /**
     * Gets the PDS server URL.
     */
    fun getServerUrl(): String = "http://localhost:3000"

    /**
     * Checks if PDS should auto-start on app launch.
     */
    fun shouldAutoStart(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_START, false)
    }

    /**
     * Sets whether PDS should auto-start on app launch.
     */
    fun setAutoStart(context: Context, autoStart: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_START, autoStart).apply()
    }
}
