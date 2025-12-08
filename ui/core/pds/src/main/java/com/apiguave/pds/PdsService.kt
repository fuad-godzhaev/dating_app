package com.apiguave.pds

import android.content.Context
import android.os.Build
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

                // Check for native module compatibility
                checkNativeModuleCompatibility(nodeProjectPath)

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

    /**
     * Checks if native Node.js modules are compatible with the device architecture.
     * Logs warnings if incompatibilities are detected.
     */
    private fun checkNativeModuleCompatibility(nodeProjectPath: File) {
        val deviceAbi = Build.SUPPORTED_ABIS[0]
        Log.i(TAG, "Device primary ABI: $deviceAbi")
        Log.i(TAG, "All supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")

        // Check for better-sqlite3 native module
        val sqliteModule = File(nodeProjectPath, "node_modules/@atproto/pds/node_modules/better-sqlite3/build/Release/better_sqlite3.node")

        if (sqliteModule.exists()) {
            Log.i(TAG, "Found better-sqlite3 native module at: ${sqliteModule.absolutePath}")

            // Try to determine if it's the right architecture by checking file size
            // This is not foolproof but gives an indication
            val fileSize = sqliteModule.length()
            Log.i(TAG, "better-sqlite3 module size: $fileSize bytes")

            // Warn about potential incompatibility
            if (deviceAbi.contains("arm") || deviceAbi.contains("aarch64")) {
                Log.w(TAG, "====================================================")
                Log.w(TAG, "WARNING: Native module compatibility issue detected!")
                Log.w(TAG, "Device ABI: $deviceAbi")
                Log.w(TAG, "The better-sqlite3 module may be compiled for a different platform.")
                Log.w(TAG, "PDS may fail to start due to architecture mismatch.")
                Log.w(TAG, "See PDS_NATIVE_MODULES_GUIDE.md for solutions.")
                Log.w(TAG, "====================================================")
            }
        } else {
            Log.w(TAG, "better-sqlite3 native module not found!")
            Log.w(TAG, "PDS will fail to start. Check that node_modules was copied correctly.")
        }
    }
}
