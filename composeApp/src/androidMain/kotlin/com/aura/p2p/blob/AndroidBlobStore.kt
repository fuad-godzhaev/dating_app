package com.aura.p2p.blob

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android [BlobStore]: files under `<filesDir>/blobs/<cid>`. The CID is a safe
 * file name (base32-lower, no separators). All IO is moved to [Dispatchers.IO].
 */
class AndroidBlobStore(private val context: Context) : BlobStore {

    private val dir: File by lazy { File(context.filesDir, "blobs").apply { mkdirs() } }

    override suspend fun write(cid: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val file = File(dir, cid)
        file.writeBytes(bytes)
        file.absolutePath
    }

    override suspend fun read(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists()) runCatching { file.readBytes() }.getOrNull() else null
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists()
    }
}
