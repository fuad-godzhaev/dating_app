package fyp.project.datingapp.p2p.relay

import fyp.project.datingapp.p2p.transport.Libp2pTransport

/**
 * Holder side of Phase F: announce (via a DHT provider record) that this device
 * relay-caches a given target DID's profile and is willing to serve it. Called by
 * [RelayPolicy] after a successful `put`. A narrow seam (the `expect` transport
 * can't be faked in commonTest); [TransportCacheHolderAdvertiser] is the
 * production adapter.
 */
interface CacheHolderAdvertiser {
    suspend fun advertise(targetDid: String)
}

/**
 * Production adapter: provider records on both [CacheHolderKeys.cacheHolderKey]
 * (Phase F profile serving) and [MailboxKeys.holderKey] (M5 offline mail), since a
 * cacheHolder doubles as the recipient's mailbox holder (ADR-0001).
 */
class TransportCacheHolderAdvertiser(private val transport: Libp2pTransport) : CacheHolderAdvertiser {
    override suspend fun advertise(targetDid: String) {
        transport.dhtProvide(CacheHolderKeys.cacheHolderKey(targetDid))
        transport.dhtProvide(MailboxKeys.holderKey(targetDid))
    }
}
