package com.aura.records.canonical

/**
 * A restricted CBOR value tree used as the input to [CanonicalEncoder].
 *
 * DAG-CBOR (https://ipld.io/specs/codecs/dag-cbor/spec/) restricts the CBOR
 * type set: only text-keyed maps, definite-length containers, and no
 * indefinite-length streams. Floats are permitted by DAG-CBOR but the records
 * in this codebase don't need them, so they are intentionally left out.
 */
sealed class CborValue {
    data class CInt(val v: Long) : CborValue()
    data class CBytes(val v: ByteArray) : CborValue() {
        override fun equals(other: Any?): Boolean =
            this === other || (other is CBytes && v.contentEquals(other.v))
        override fun hashCode(): Int = v.contentHashCode()
    }
    data class CString(val v: String) : CborValue()
    data class CArray(val v: List<CborValue>) : CborValue()
    /** Text-keyed map per DAG-CBOR. Insertion order is irrelevant — the encoder sorts. */
    data class CMap(val v: Map<String, CborValue>) : CborValue()
    data class CBool(val v: Boolean) : CborValue()
    data object CNull : CborValue()
}
