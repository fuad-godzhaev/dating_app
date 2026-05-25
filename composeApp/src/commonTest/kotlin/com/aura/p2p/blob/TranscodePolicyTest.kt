package com.aura.p2p.blob

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Device-independent transcode decisions (the pure half of the photo-compression change). */
class TranscodePolicyTest {

    @Test
    fun skipsSmallImages() {
        // Within the edge cap AND small in bytes -> leave it alone (avoid re-encode quality loss).
        assertFalse(TranscodePolicy.shouldTranscode(longestEdgePx = 1024, byteCount = 120_000))
    }

    @Test
    fun transcodesOversizedDimensions() {
        assertTrue(TranscodePolicy.shouldTranscode(longestEdgePx = 4032, byteCount = 50_000))
    }

    @Test
    fun transcodesHeavyBytesEvenIfSmallDims() {
        assertTrue(TranscodePolicy.shouldTranscode(longestEdgePx = 1000, byteCount = 1_500_000))
    }

    @Test
    fun targetDimensionsScaleLongestEdgeAndKeepAspect() {
        // 4032x3024 (4:3) -> longest edge 1280, aspect preserved.
        val (w, h) = TranscodePolicy.targetDimensions(4032, 3024)
        assertEquals(1280, w)
        assertEquals(960, h)
    }

    @Test
    fun targetDimensionsNeverUpscale() {
        val (w, h) = TranscodePolicy.targetDimensions(800, 600)
        assertEquals(800, w)
        assertEquals(600, h)
    }

    @Test
    fun targetDimensionsHandlesPortrait() {
        val (w, h) = TranscodePolicy.targetDimensions(3024, 4032)
        assertEquals(1280, h)
        assertEquals(960, w)
    }

    @Test
    fun inSampleSizeIsPowerOfTwoAndKeepsAboveTarget() {
        // 4032 longest -> /2=2016 (>=1280), /2 again=1008 (<1280) so stop at sample=2.
        assertEquals(2, TranscodePolicy.inSampleSize(4032, 3024))
        // Already small -> no subsample.
        assertEquals(1, TranscodePolicy.inSampleSize(1000, 800))
        // 8000 -> 4000 -> 2000 (>=1280), next 1000 (<1280) -> sample=4.
        assertEquals(4, TranscodePolicy.inSampleSize(8000, 6000))
    }
}
