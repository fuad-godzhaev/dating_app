package com.aura.p2p.blob

/**
 * iOS [ImageTranscoder]: passthrough for now (Aura is Android-primary). A UIImage-based
 * downscale/recompress can replace this later; returning the original bytes keeps the
 * content-addressed upload path correct on iOS in the meantime.
 */
class IosImageTranscoder : ImageTranscoder {
    override suspend fun transcode(bytes: ByteArray, mimeType: String): TranscodedImage =
        TranscodedImage(bytes, mimeType, 0, 0)
}
