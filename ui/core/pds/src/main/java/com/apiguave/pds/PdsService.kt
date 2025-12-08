package com.apiguave.pds

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Service to manage the ATProto PDS (Personal Data Server) running on Node.js.
 *
 * This service uses direct JNI to run Node.js natively on Android.
 */
class PdsService(private val context: Context) {

    private var isRunning = false

    companion object {
        private const val TAG = "PdsService"
        private const val NODE_PROJECT_DIR = "nodejs-project"
        private const val PDS_PORT = 3000
    }

    /**
     * Starts the PDS Node.js server.
     *
     * @param onStarted Callback invoked when server has started
     * @param onError Callback invoked if there's an error
     */
    fun start(
        onStarted: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (isRunning) {
            Log.w(TAG, "PDS is already running")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "Starting PDS Node.js runtime...")

                // Check if nodejs-project exists in internal storage
                val nodeProjectPath = File(context.filesDir, NODE_PROJECT_DIR)
                if (!nodeProjectPath.exists() || !File(nodeProjectPath, "index.js").exists()) {
                    Log.i(TAG, "Copying Node.js project from assets...")
                    copyNodeProjectFromAssets()
                }

                // Start Node.js runtime with the PDS script
                val scriptPath = File(nodeProjectPath, "index.js").absolutePath
                Log.i(TAG, "Starting Node.js with script: $scriptPath")

                // Set working directory via system property (Node.js will read this)
                System.setProperty("user.dir", nodeProjectPath.absolutePath)

                val success = NodeJsBridge.startNode(scriptPath)

                if (!success) {
                    throw Exception("Failed to start Node.js runtime")
                }

                isRunning = true

                // Give Node.js a moment to start up
                delay(3000)

                Log.i(TAG, "PDS started successfully on port $PDS_PORT")
                onStarted?.invoke()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start PDS", e)
                isRunning = false
                onError?.invoke(e.message ?: "Failed to start PDS")
            }
        }
    }

    /**
     * Stops the PDS Node.js server.
     */
    fun stop() {
        if (!isRunning) {
            Log.w(TAG, "PDS is not running")
            return
        }

        try {
            Log.i(TAG, "Stopping PDS...")
            NodeJsBridge.stopNode()
            isRunning = false
            Log.i(TAG, "PDS stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping PDS", e)
        }
    }

    /**
     * Checks if PDS is currently running.
     */
    fun isRunning(): Boolean = isRunning && NodeJsBridge.isNodeRunning()

    /**
     * Gets the PDS server URL.
     */
    fun getServerUrl(): String = "http://localhost:$PDS_PORT"

    /**
     * Copies the Node.js project from assets to internal storage.
     * This is necessary because Node.js needs to read from the filesystem.
     */
    private fun copyNodeProjectFromAssets() {
        Log.i(TAG, "Copying Node.js project from assets...")

        val nodeProjectPath = File(context.filesDir, NODE_PROJECT_DIR)
        if (nodeProjectPath.exists()) {
            nodeProjectPath.deleteRecursively()
        }
        nodeProjectPath.mkdirs()

        try {
            // Copy all files from assets/nodejs-project to internal storage
            val assetFiles = context.assets.list(NODE_PROJECT_DIR)
            if (assetFiles.isNullOrEmpty()) {
                throw Exception("No files found in assets/$NODE_PROJECT_DIR")
            }

            assetFiles.forEach { filename ->
                copyAssetFolder(NODE_PROJECT_DIR, filename, nodeProjectPath)
            }

            Log.i(TAG, "Node.js project copied successfully to ${nodeProjectPath.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error copying Node.js project", e)
            throw e
        }
    }

    /**
     * Recursively copies asset folders and files.
     */
    private fun copyAssetFolder(assetPath: String, filename: String, targetDir: File) {
        val fullAssetPath = "$assetPath/$filename"
        val assets = context.assets.list(fullAssetPath)

        if (assets.isNullOrEmpty()) {
            // It's a file
            val targetFile = File(targetDir, filename)
            context.assets.open(fullAssetPath).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Copied file: $fullAssetPath -> ${targetFile.absolutePath}")
        } else {
            // It's a directory
            val targetSubDir = File(targetDir, filename)
            targetSubDir.mkdirs()
            assets.forEach { subFilename ->
                copyAssetFolder(fullAssetPath, subFilename, targetSubDir)
            }
        }
    }
}
