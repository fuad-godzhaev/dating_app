package fyp.project.datingapp.records.canonical

/**
 * Deterministic DAG-CBOR encoder.
 *
 * Conforms to:
 *  - RFC 8949 §4.2.1 (Core Deterministic Encoding Requirements)
 *  - DAG-CBOR strictness rules (https://ipld.io/specs/codecs/dag-cbor/spec/)
 *
 * Guarantees, per call:
 *  - Integer heads are encoded with the smallest admissible width
 *    (immediate / 1 / 2 / 4 / 8 bytes).
 *  - All containers use definite-length encoding.
 *  - Map keys are sorted by the byte-length of their encoded form first,
 *    then lexicographically by those encoded bytes (RFC 8949 §4.2.1 rule 3).
 *    Because keys are always text strings in DAG-CBOR, and because CBOR
 *    text-string heads encode the length before the UTF-8 body, sorting the
 *    encoded-header-plus-body is equivalent to sorting by (utf8 length, utf8
 *    bytes) — which is what the code does for efficiency.
 *  - No floats, no tags, no indefinite-length items are emitted.
 *
 * Not a full CBOR decoder: this module only encodes the closed set of values
 * in [CborValue]. That keeps the implementation small and auditable.
 */
object CanonicalEncoder {

    fun encode(value: CborValue): ByteArray {
        val out = ByteBuffer()
        writeValue(out, value)
        return out.toByteArray()
    }

    // ---- major types ---------------------------------------------------------
    // 0: unsigned int, 1: negative int, 2: bytes, 3: text, 4: array, 5: map,
    // 7: simple (false=20, true=21, null=22)

    private fun writeValue(out: ByteBuffer, v: CborValue) {
        when (v) {
            is CborValue.CInt -> writeInt(out, v.v)
            is CborValue.CBytes -> {
                writeHead(out, major = 2, argument = v.v.size.toLong())
                out.write(v.v)
            }
            is CborValue.CString -> {
                val utf8 = v.v.encodeToByteArray()
                writeHead(out, major = 3, argument = utf8.size.toLong())
                out.write(utf8)
            }
            is CborValue.CArray -> {
                writeHead(out, major = 4, argument = v.v.size.toLong())
                v.v.forEach { writeValue(out, it) }
            }
            is CborValue.CMap -> writeMap(out, v.v)
            is CborValue.CBool -> out.write(if (v.v) 0xF5.toByte() else 0xF4.toByte())
            CborValue.CNull -> out.write(0xF6.toByte())
        }
    }

    private fun writeInt(out: ByteBuffer, n: Long) {
        if (n >= 0L) {
            writeHead(out, major = 0, argument = n)
        } else {
            // CBOR negative: major type 1, argument = -1 - n
            // Using unsigned math: for n = Long.MIN_VALUE, (-1 - n) is representable
            // as an unsigned 64-bit value (2^63 - 1)... actually -1 - Long.MIN_VALUE
            // overflows in two's-complement signed math but yields the correct
            // unsigned pattern when re-interpreted. Our records never reach that
            // boundary (timestamps, ages, sizes — all far from Long.MIN_VALUE).
            val arg = -1L - n
            writeHead(out, major = 1, argument = arg)
        }
    }

    private fun writeMap(out: ByteBuffer, m: Map<String, CborValue>) {
        // Pre-encode keys to bytes so sorting is cheap and matches wire order.
        val entries = m.entries.map { (k, v) -> k.encodeToByteArray() to v }
        // DAG-CBOR / RFC 8949 §4.2.1 length-first lex sort over encoded key bytes.
        val sorted = entries.sortedWith(compareBy<Pair<ByteArray, CborValue>> { it.first.size }
            .then(ByteArrayLexComparator { it.first }))
        writeHead(out, major = 5, argument = sorted.size.toLong())
        for ((keyBytes, v) in sorted) {
            writeHead(out, major = 3, argument = keyBytes.size.toLong())
            out.write(keyBytes)
            writeValue(out, v)
        }
    }

    /**
     * Write a type-3-bit / argument head using the shortest valid width.
     * The argument is carried as a non-negative Long; callers must have already
     * mapped CBOR negative integers into their unsigned argument form.
     */
    private fun writeHead(out: ByteBuffer, major: Int, argument: Long) {
        val tag = (major and 0x07) shl 5
        when {
            argument < 0L -> error("negative argument in CBOR head")
            argument < 24L -> out.write((tag or argument.toInt()).toByte())
            argument < 0x100L -> {
                out.write((tag or 24).toByte())
                out.write(argument.toByte())
            }
            argument < 0x10000L -> {
                out.write((tag or 25).toByte())
                out.write((argument ushr 8).toByte())
                out.write(argument.toByte())
            }
            argument < 0x100000000L -> {
                out.write((tag or 26).toByte())
                out.write((argument ushr 24).toByte())
                out.write((argument ushr 16).toByte())
                out.write((argument ushr 8).toByte())
                out.write(argument.toByte())
            }
            else -> {
                out.write((tag or 27).toByte())
                for (shift in 56 downTo 0 step 8) out.write((argument ushr shift).toByte())
            }
        }
    }
}

// ---- private helpers -------------------------------------------------------

/** Grow-on-demand byte buffer. Avoids pulling in a platform dep just for this. */
private class ByteBuffer(initialCapacity: Int = 128) {
    private var buf = ByteArray(initialCapacity)
    private var size = 0
    fun write(b: Byte) {
        ensure(1); buf[size++] = b
    }
    fun write(bytes: ByteArray) {
        ensure(bytes.size); bytes.copyInto(buf, size); size += bytes.size
    }
    fun toByteArray(): ByteArray = buf.copyOf(size)
    private fun ensure(extra: Int) {
        if (size + extra <= buf.size) return
        var newSize = buf.size
        while (newSize < size + extra) newSize *= 2
        buf = buf.copyOf(newSize)
    }
}

private class ByteArrayLexComparator<T>(private val extract: (T) -> ByteArray) : Comparator<T> {
    override fun compare(a: T, b: T): Int {
        val x = extract(a); val y = extract(b)
        val len = minOf(x.size, y.size)
        for (i in 0 until len) {
            val cmp = (x[i].toInt() and 0xFF) - (y[i].toInt() and 0xFF)
            if (cmp != 0) return cmp
        }
        return x.size - y.size
    }
}
