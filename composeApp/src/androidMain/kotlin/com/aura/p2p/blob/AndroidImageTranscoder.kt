package com.aura.p2p.blob

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android [ImageTranscoder]: decodes with a subsampling factor (low peak memory), scales the
 * longest edge down to [TranscodePolicy.MAX_EDGE_PX], and re-encodes lossy (WEBP on API 30+, else
 * JPEG) at [TranscodePolicy.QUALITY]. Falls back to the original bytes on any failure or if the
 * policy says the image is already small enough - the caller's content-addressing then just hashes
 * whatever it returns, so a fallback is always safe.
 */
class AndroidImageTranscoder : ImageTranscoder {

    override suspend fun transcode(bytes: ByteArray, mimeType: String): TranscodedImage =
        withContext(Dispatchers.Default) {
            runCatching { transcodeOrThrow(bytes, mimeType) }
                .getOrDefault(TranscodedImage(bytes, mimeType, 0, 0))
        }

    private fun transcodeOrThrow(bytes: ByteArray, mimeType: String): TranscodedImage {
        // 1) Read dimensions only.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return TranscodedImage(bytes, mimeType, 0, 0) // not a decodable image

        if (!TranscodePolicy.shouldTranscode(maxOf(srcW, srcH), bytes.size)) {
            return TranscodedImage(bytes, mimeType, srcW, srcH)
        }

        // 2) Decode subsampled, then scale to the exact target.
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = TranscodePolicy.inSampleSize(srcW, srcH)
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            ?: return TranscodedImage(bytes, mimeType, srcW, srcH)
        val (tw, th) = TranscodePolicy.targetDimensions(decoded.width, decoded.height)
        val scaled = if (tw != decoded.width || th != decoded.height) {
            Bitmap.createScaledBitmap(decoded, tw, th, true).also { if (it != decoded) decoded.recycle() }
        } else {
            decoded
        }

        // 3) Re-encode lossy.
        val (format, outMime) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP_LOSSY to "image/webp"
        } else {
            Bitmap.CompressFormat.JPEG to "image/jpeg"
        }
        val out = ByteArrayOutputStream()
        scaled.compress(format, TranscodePolicy.QUALITY, out)
        val w = scaled.width
        val h = scaled.height
        scaled.recycle()
        val encoded = out.toByteArray()

        // 4) Safety: if re-encoding somehow grew the image, keep the original.
        return if (encoded.isNotEmpty() && encoded.size < bytes.size) {
            TranscodedImage(encoded, outMime, w, h)
        } else {
            TranscodedImage(bytes, mimeType, srcW, srcH)
        }
    }
}
