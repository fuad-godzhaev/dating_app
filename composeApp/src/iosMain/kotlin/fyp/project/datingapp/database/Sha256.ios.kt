package fyp.project.datingapp.database

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual fun sha256Digest(data: ByteArray): ByteArray {
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
    data.usePinned { inputPinned ->
        digest.usePinned { outputPinned ->
            CC_SHA256(
                inputPinned.addressOf(0),
                data.size.toUInt(),
                outputPinned.addressOf(0)
            )
        }
    }
    return digest.toByteArray()
}
