package com.apiguave.pds

import android.util.Log

/**
 * JNI bridge to Node.js native library.
 *
 * This class provides direct access to the libnode.so library.
 */
object NodeJsBridge {

    private const val TAG = "NodeJsBridge"

    init {
        try {
            System.loadLibrary("node")
            System.loadLibrary("pds_node_bridge")
            Log.i(TAG, "Native libraries loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native libraries", e)
            throw e
        }
    }

    /**
     * Starts the Node.js runtime with the specified script.
     *
     * @param scriptPath Absolute path to the JavaScript file to execute
     * @return true if Node.js started successfully, false otherwise
     */
    external fun startNode(scriptPath: String): Boolean

    /**
     * Checks if Node.js is currently running.
     *
     * @return true if Node.js is running, false otherwise
     */
    external fun isNodeRunning(): Boolean

    /**
     * Stops the Node.js runtime.
     *
     * Note: This does not perform a graceful shutdown of the Node.js event loop.
     * For production use, implement IPC to send a shutdown signal to the script.
     */
    external fun stopNode()
}
