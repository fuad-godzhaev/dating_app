package fyp.project.datingapp.records.canonical

/**
 * Minimal CBOR decoder into [CborValue]. Accepts the subset of CBOR this
 * codebase emits via [CanonicalEncoder]:
 *
 *   - unsigned / negative integers (up to 64-bit)
 *   - byte strings (definite length)
 *   - text strings (definite length)
 *   - arrays / maps (definite length)
 *   - false / true / null
 *
 * Anything outside that subset — indefinite-length items, floats, tags,
 * reserved major types — is rejected. This is a feature, not a limitation:
 * the decoder is only meant to round-trip our own canonical output. Foreign
 * records that reach the decoder are likely malformed or adversarial.
 */
object CanonicalDecoder {

    fun decode(bytes: ByteArray): CborValue {
        val r = Reader(bytes)
        val v = readValue(r)
        if (r.hasMore()) error("trailing bytes after canonical CBOR value")
        return v
    }

    private fun readValue(r: Reader): CborValue {
        val initial = r.readByte().toInt() and 0xFF
        val major = initial ushr 5
        val info = initial and 0x1F
        return when (major) {
            0 -> CborValue.CInt(readUnsignedArgument(r, info))
            1 -> {
                val arg = readUnsignedArgument(r, info)
                // CBOR: value = -1 - arg. For arg = Long.MAX_VALUE this returns
                // Long.MIN_VALUE, which is fine for our records (we never encode
                // values near those boundaries).
                CborValue.CInt(-1L - arg)
            }
            2 -> {
                val len = readUnsignedArgument(r, info)
                requireFits(len)
                CborValue.CBytes(r.readBytes(len.toInt()))
            }
            3 -> {
                val len = readUnsignedArgument(r, info)
                requireFits(len)
                CborValue.CString(r.readBytes(len.toInt()).decodeToString())
            }
            4 -> {
                val len = readUnsignedArgument(r, info)
                requireFits(len)
                val items = ArrayList<CborValue>(len.toInt())
                repeat(len.toInt()) { items.add(readValue(r)) }
                CborValue.CArray(items)
            }
            5 -> {
                val len = readUnsignedArgument(r, info)
                requireFits(len)
                val map = LinkedHashMap<String, CborValue>(len.toInt())
                repeat(len.toInt()) {
                    val key = readValue(r)
                    require(key is CborValue.CString) { "DAG-CBOR map keys must be text strings" }
                    map[key.v] = readValue(r)
                }
                CborValue.CMap(map)
            }
            7 -> when (info) {
                20 -> CborValue.CBool(false)
                21 -> CborValue.CBool(true)
                22 -> CborValue.CNull
                else -> error("unsupported simple/float type (info=$info)")
            }
            else -> error("unsupported major type $major")
        }
    }

    private fun readUnsignedArgument(r: Reader, info: Int): Long {
        return when {
            info < 24 -> info.toLong()
            info == 24 -> (r.readByte().toInt() and 0xFF).toLong()
            info == 25 -> ((r.readByte().toInt() and 0xFF).toLong() shl 8) or
                    (r.readByte().toInt() and 0xFF).toLong()
            info == 26 -> {
                var v = 0L
                repeat(4) { v = (v shl 8) or (r.readByte().toLong() and 0xFFL) }
                v
            }
            info == 27 -> {
                var v = 0L
                repeat(8) { v = (v shl 8) or (r.readByte().toLong() and 0xFFL) }
                v
            }
            else -> error("indefinite length / reserved info byte: $info")
        }
    }

    private fun requireFits(len: Long) {
        require(len in 0..Int.MAX_VALUE) { "CBOR length $len exceeds Int.MAX_VALUE" }
    }

    private class Reader(private val buf: ByteArray) {
        private var i = 0
        fun hasMore(): Boolean = i < buf.size
        fun readByte(): Byte {
            if (i >= buf.size) error("unexpected end of CBOR input")
            return buf[i++]
        }
        fun readBytes(n: Int): ByteArray {
            if (i + n > buf.size) error("unexpected end of CBOR input")
            val out = buf.copyOfRange(i, i + n)
            i += n
            return out
        }
    }
}

