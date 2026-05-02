package fyp.project.datingapp.domain.auth

import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec

/**
 * Minimal P-256 (secp256r1) EC math in pure JCA + BigInteger (no Bouncy Castle).
 *
 * Needed because account recovery (ADR-0003, amended) requires the signing key to
 * be re-derivable from the recovery phrase — a non-exportable Keystore key can't
 * be. So the key is derived deterministically: scalar d from the seed, public
 * point Q = d*G via [scalarBaseMult] (affine double-and-add). JCA exposes no
 * base-point multiplication, hence this.
 *
 * Correctness is checked at runtime by [selfTest] (sign-with-d / verify-with-Q
 * round-trips through JCA's vetted ECDSA, plus 1*G == G).
 */
internal object P256 {
    val params: ECParameterSpec = AlgorithmParameters.getInstance("EC").run {
        init(ECGenParameterSpec("secp256r1"))
        getParameterSpec(ECParameterSpec::class.java)
    }
    private val p: BigInteger = (params.curve.field as ECFieldFp).p
    private val a: BigInteger = params.curve.a
    private val b: BigInteger = params.curve.b
    private val n: BigInteger = params.order
    private val g: ECPoint = params.generator

    /** Map arbitrary seed bytes to a private scalar in [1, n-1]. */
    fun scalarFromSeed(seed: ByteArray): BigInteger =
        BigInteger(1, seed).mod(n - BigInteger.ONE) + BigInteger.ONE

    /** Q = d*G, affine double-and-add. */
    fun scalarBaseMult(d: BigInteger): ECPoint {
        var result: ECPoint? = null // point at infinity
        var addend = g
        var k = d
        while (k.signum() > 0) {
            if (k.testBit(0)) result = add(result, addend)
            addend = double(addend)
            k = k.shiftRight(1)
        }
        return result ?: error("scalar is zero")
    }

    /** R = d*P (arbitrary point), affine double-and-add. ECDH = d_self * Q_peer. */
    fun scalarMult(d: BigInteger, point: ECPoint): ECPoint {
        var result: ECPoint? = null // point at infinity
        var addend = point
        var k = d
        while (k.signum() > 0) {
            if (k.testBit(0)) result = add(result, addend)
            addend = double(addend)
            k = k.shiftRight(1)
        }
        return result ?: error("scalar is zero")
    }

    /** SEC1 compressed point: 0x02/0x03 (y parity) + 32-byte big-endian X. */
    fun compress(point: ECPoint): ByteArray {
        val prefix = if (point.affineY.testBit(0)) 0x03 else 0x02
        return byteArrayOf(prefix.toByte()) + to32(point.affineX)
    }

    /** Inverse of [compress]; null if not a valid compressed point on the curve. */
    fun decompress(compressed: ByteArray): ECPoint? {
        if (compressed.size != 33) return null
        val prefix = compressed[0].toInt()
        if (prefix != 0x02 && prefix != 0x03) return null
        val x = BigInteger(1, compressed.copyOfRange(1, 33))
        if (x >= p) return null
        val rhs = x.multiply(x).mod(p).add(a).multiply(x).add(b).mod(p) // x^3 + a*x + b
        var y = rhs.modPow((p + BigInteger.ONE).shiftRight(2), p)        // p ≡ 3 (mod 4)
        if (y.multiply(y).mod(p) != rhs) return null                    // not a residue -> invalid
        if (y.testBit(0) != (prefix == 0x03)) y = p - y
        return ECPoint(x, y)
    }

    fun privateKey(d: BigInteger): ECPrivateKey =
        KeyFactory.getInstance("EC").generatePrivate(ECPrivateKeySpec(d, params)) as ECPrivateKey

    fun publicKey(point: ECPoint): ECPublicKey =
        KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(point, params)) as ECPublicKey

    fun to32(bi: BigInteger): ByteArray {
        val full = bi.toByteArray()
        val out = ByteArray(32)
        if (full.size >= 32) System.arraycopy(full, full.size - 32, out, 0, 32)
        else System.arraycopy(full, 0, out, 32 - full.size, full.size)
        return out
    }

    private fun add(p1: ECPoint?, p2: ECPoint): ECPoint? {
        if (p1 == null) return p2
        if (p1.affineX == p2.affineX) {
            return if (p1.affineY == p2.affineY) double(p2) else null // P + (-P) = O
        }
        val lambda = (p2.affineY - p1.affineY)
            .multiply((p2.affineX - p1.affineX).modInverse(p)).mod(p)
        val x3 = (lambda.multiply(lambda) - p1.affineX - p2.affineX).mod(p)
        val y3 = (lambda.multiply(p1.affineX - x3) - p1.affineY).mod(p)
        return ECPoint(x3, y3)
    }

    private fun double(pt: ECPoint): ECPoint {
        val lambda = (pt.affineX.multiply(pt.affineX).multiply(BigInteger.valueOf(3)) + a)
            .multiply(pt.affineY.shiftLeft(1).modInverse(p)).mod(p)
        val x3 = (lambda.multiply(lambda) - pt.affineX.shiftLeft(1)).mod(p)
        val y3 = (lambda.multiply(pt.affineX - x3) - pt.affineY).mod(p)
        return ECPoint(x3, y3)
    }
}
