package fyp.project.datingapp.p2p.relay

import fyp.project.datingapp.domain.auth.SignatureVerifier
import fyp.project.datingapp.p2p.transport.wire.ProfileInvalidation
import fyp.project.datingapp.records.canonical.decodeProfileInvalidation
import fyp.project.datingapp.records.canonical.encodeInvalidationWire
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * GossipSub-driven cache invalidation (`p2p-subsystem-design.md` §8.4 / §11.3).
 *
 * For every DID this device relay-caches, [RelayPolicy] calls [subscribe] so the
 * invalidator listens on `fyp.project.datingapp.invalidate/v1/<did>`. Each
 * inbound [ProfileInvalidation] is verified against the owner's key
 * ([ProfileInvalidationVerifier]) and, if valid, handed to
 * [RelayPolicy.onInvalidationReplace] — which swaps the cached envelope for the
 * new version in place, with no pull round-trip. An owner calls [publish] after
 * saving a new profile version to notify holders.
 *
 * One collector job per DID, tracked in [jobs] under [mutex] so concurrent
 * subscribe/unsubscribe can't leak or double-start a collector.
 */
class DefaultGossipSubInvalidator(
    private val channel: GossipChannel,
    private val verifier: SignatureVerifier,
    private val relayPolicy: RelayPolicy,
    private val scope: CoroutineScope,
) : GossipSubInvalidator {

    private val mutex = Mutex()
    private val jobs = mutableMapOf<String, Job>()

    override suspend fun subscribe(did: String) {
        mutex.withLock {
            if (jobs.containsKey(did)) return
            val topic = InvalidationTopics.topic(did)
            val job = scope.launch {
                runCatching {
                    channel.subscribe(topic).collect { message ->
                        val invalidation = runCatching { decodeProfileInvalidation(message.payload) }
                            .getOrNull() ?: return@collect
                        // A topic carries one DID's invalidations; ignore cross-posts.
                        if (invalidation.ownerDid != did) return@collect
                        if (!ProfileInvalidationVerifier.verify(invalidation, verifier)) return@collect
                        runCatching { relayPolicy.onInvalidationReplace(invalidation) }
                    }
                }
            }
            jobs[did] = job
        }
    }

    override suspend fun unsubscribe(did: String) {
        mutex.withLock {
            jobs.remove(did)?.cancel()
        }
        runCatching { channel.unsubscribe(InvalidationTopics.topic(did)) }
    }

    override suspend fun publish(invalidation: ProfileInvalidation) {
        runCatching {
            channel.publish(
                InvalidationTopics.topic(invalidation.ownerDid),
                encodeInvalidationWire(invalidation),
            )
        }
    }
}
