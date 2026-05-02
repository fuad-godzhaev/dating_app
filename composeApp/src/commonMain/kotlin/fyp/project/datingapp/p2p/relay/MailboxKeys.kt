package fyp.project.datingapp.p2p.relay

import fyp.project.datingapp.database.sha256Digest

/**
 * DHT key under which mailbox holders advertise (via provider records) that they
 * queue offline mail for a recipient (ADR-0001 / M5). A holder calls
 * `dhtProvide(holderKey(recipientDid))`; a sender (to park mail) or the recipient
 * (to collect it) calls `dhtFindProviders(holderKey(recipientDid))`. Mirrors
 * [CacheHolderKeys]; cacheHolders double as mailbox holders.
 */
object MailboxKeys {
    const val KEY_PREFIX = "fyp.project.datingapp.mailbox-holders.v1/"

    fun holderKey(recipientDid: String): ByteArray =
        sha256Digest((KEY_PREFIX + recipientDid).encodeToByteArray())
}
