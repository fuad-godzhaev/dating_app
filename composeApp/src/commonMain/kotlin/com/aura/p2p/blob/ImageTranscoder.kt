package com.aura.p2p.blob

/**
 * Downscales + recompresses a picked image before it is stored as a content-addressed blob.
 *
 * Rationale (sim-results phase2): profile photos were stored at full resolution with NO
 * compression (PhotoUploader wrote the picked bytes verbatim), ~2 MB each. The throughput model
 * showed photo serving is the binding holder bottleneck (~16x the cost of mailbox traffic, a
 * cellular serve window clears only ~35 full profiles), and a 20-profile feed browse moved ~48 MB.
 * Transcoding to a ~1280 px longest edge at quality ~80 cuts a photo to ~200-400 KB (~5-10x),
 * which relaxes the holder throughput wall, the feed-browse cost, the holder data-plan burn, and
 * cold-start latency - the single highest-leverage efficiency change.
 *
 * The platform implementations do the actual pixel work (Android: Bitmap; iOS: passthrough for
 * now). The pure [TranscodePolicy] holds the device-independent decisions so they are unit-tested
 * without a platform image library.
 */
interface ImageTranscoder {
    /** Decode [bytes], downscale per [TranscodePolicy], re-encode; returns the transcoded image.
     *  Implementations MUST fall back to the original bytes on any decode/encode failure. */
    suspend fun transcode(bytes: ByteArray, mimeType: String): TranscodedImage
}

/** Result of a transcode: the (possibly recompressed) [bytes] and their [mimeType] + dimensions. */
class TranscodedImage(
    val bytes: ByteArray,
    val mimeType: String,
    val width: Int,
    val height: Int,
)

/**
 * Device-independent transcode decisions (pure, unit-tested). Tuned from the size/throughput
 * analysis: a 1280 px longest edge at quality 80 is ample for a phone-screen profile photo while
 * cutting bytes ~5-10x.
 */
object TranscodePolicy {
    /** Longest-edge target in pixels. Images larger than this are downscaled to it. */
    const val MAX_EDGE_PX: Int = 1280

    /** Lossy re-encode quality (0-100). 80 is visually clean for photos at a large size saving. */
    const val QUALITY: Int = 80

    /** Below this size AND already within [MAX_EDGE_PX], skip re-encoding (avoid quality loss on
     *  already-small images). */
    const val SKIP_BELOW_BYTES: Int = 400_000

    /** Whether transcoding is worthwhile: oversized in pixels, or large in bytes. */
    fun shouldTranscode(longestEdgePx: Int, byteCount: Int): Boolean =
        longestEdgePx > MAX_EDGE_PX || byteCount > SKIP_BELOW_BYTES

    /**
     * BitmapFactory `inSampleSize` (a power of two) to decode at, so the decoded longest edge is
     * still >= [MAX_EDGE_PX] (we then scale to exact). Decoding subsampled keeps peak memory low.
     */
    fun inSampleSize(srcW: Int, srcH: Int, maxEdge: Int = MAX_EDGE_PX): Int {
        var sample = 1
        var longest = maxOf(srcW, srcH)
        // Halve while the next halving would still leave us >= maxEdge.
        while (longest / 2 >= maxEdge) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    /** Final dimensions after scaling the longest edge down to [MAX_EDGE_PX] (never upscales). */
    fun targetDimensions(srcW: Int, srcH: Int, maxEdge: Int = MAX_EDGE_PX): Pair<Int, Int> {
        if (srcW <= 0 || srcH <= 0) return srcW to srcH
        val longest = maxOf(srcW, srcH)
        if (longest <= maxEdge) return srcW to srcH
        val scale = maxEdge.toDouble() / longest
        val w = (srcW * scale).toInt().coerceAtLeast(1)
        val h = (srcH * scale).toInt().coerceAtLeast(1)
        return w to h
    }
}
