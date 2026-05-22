package fyp.project.datingapp.p2p.transport

import android.content.Context
import android.util.Log
import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.domain.auth.AuthRepository
import fyp.project.datingapp.domain.auth.P256
import fyp.project.datingapp.domain.auth.defaultSignatureVerifier
import fyp.project.datingapp.records.UserProfile
import fyp.project.datingapp.p2p.ble.BleBeacon
import fyp.project.datingapp.p2p.ble.BleProximity
import fyp.project.datingapp.p2p.discovery.DiscoveryFilters
import fyp.project.datingapp.p2p.discovery.DiscoveryService
import fyp.project.datingapp.p2p.discovery.GeohashLocator
import fyp.project.datingapp.p2p.discovery.PresenceAnnouncer
import fyp.project.datingapp.p2p.fetch.ProfileFetcher
import fyp.project.datingapp.p2p.fetch.StreamProfileFetcher
import fyp.project.datingapp.p2p.feed.PeerProfileFeed
import fyp.project.datingapp.database.appView.dao.MessageDao
import fyp.project.datingapp.p2p.discovery.PeerDirectory
import fyp.project.datingapp.p2p.like.LikeService
import fyp.project.datingapp.p2p.messaging.MailboxService
import fyp.project.datingapp.p2p.messaging.MessageService
import fyp.project.datingapp.p2p.transport.wire.AgeRange
import java.math.BigInteger
import java.security.Signature
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.security.SecureRandom

/**
 * Debug-only B5 smoke test (gated by an intent extra in MainActivity). Spins up
 * two go-libp2p hosts in-process on loopback and exercises the transport end to
 * end: host boot (B3), a /datingapp/ping round-trip, a DHT put/get, and a
 * GossipSub publish/subscribe. Results go to logcat (tag "P2pSmoke"); read with
 * `adb logcat -s P2pSmoke`.
 *
 * Two physical emulators + mDNS discovery (the literal B5) is deferred: only one
 * AVD exists and Android mDNS needs a held WifiManager.MulticastLock. This
 * in-process variant proves dial + streams + DHT + GossipSub deterministically.
 */
object P2pSmoke {
    private const val TAG = "P2pSmoke"
    private const val PING = "/datingapp/ping/1.0.0"
    private const val TOPIC = "/datingapp/smoke"
    private const val XKEY = "xproc-key"
    private const val XVAL = "xproc-value"

    private fun seed() = ByteArray(32).also { SecureRandom().nextBytes(it) }

    /**
     * P256 correctness self-check (recovery key derivation): 1*G==G, a
     * sign-with-d / verify-with-Q round-trip through JCA ECDSA (proves Q=d*G), and
     * determinism (same seed -> same DID). Logcat tag "P2pSmoke".
     */
    fun keyCheck() {
        val g1 = P256.compress(P256.scalarBaseMult(BigInteger.ONE))
        val gRef = P256.compress(P256.params.generator)
        Log.i(TAG, "KEYCHECK 1*G==G: ${if (g1.contentEquals(gRef)) "PASS" else "FAIL"}")

        val s = ByteArray(32) { (it + 1).toByte() }
        val d = P256.scalarFromSeed(s)
        val q = P256.compress(P256.scalarBaseMult(d))
        val msg = "recovery-check".encodeToByteArray()
        val sig = Signature.getInstance("SHA256withECDSA").run { initSign(P256.privateKey(d)); update(msg); sign() }
        Log.i(TAG, "KEYCHECK sign(d)/verify(Q): ${if (defaultSignatureVerifier().verify(q, msg, sig)) "PASS" else "FAIL"}")

        val q2 = P256.compress(P256.scalarBaseMult(P256.scalarFromSeed(s)))
        Log.i(TAG, "KEYCHECK determinism: ${if (q.contentEquals(q2)) "PASS" else "FAIL"} did=${PeerIdentity.didKeyFromP256(q)}")
        Log.i(TAG, "KEYCHECK DONE")
    }

    /**
     * Cross-process SERVER role (emulator A). Listens on a fixed port, echoes
     * pings, seeds a DHT value, and publishes on the gossip topic in a loop.
     * Stays alive (does not stop). Two emulators are network-isolated, so reach
     * this from B via `adb forward tcp:<port>` + dialling /ip4/10.0.2.2/...
     */
    suspend fun runServer(port: Int, listenHost: String = "0.0.0.0") {
        // Listening on a specific IP (the wlan0 address) makes libp2p advertise that
        // address over mDNS; on Android, 0.0.0.0 often only yields 127.0.0.1 because
        // interface enumeration is restricted.
        val t = Libp2pTransport(Libp2pConfig(seed(), listenAddrs = listOf("/ip4/$listenHost/tcp/$port")))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        t.start()
        Log.i(TAG, "SERVER peerId=${t.peerId} addrs=${t.listenAddrs}")
        t.registerStreamHandler(PING, object : StreamHandler {
            override suspend fun handle(stream: Libp2pStream) {
                val req = stream.readBytes()
                stream.writeBytes(req)
                stream.close()
            }
        })
        t.gossipSubscribe(TOPIC).launchIn(scope) // join the mesh
        // Re-put + publish in a loop: a DHT put with zero peers fails, so this only
        // lands once the client has connected (peer enters the routing table).
        scope.launch {
            while (true) {
                runCatching { t.dhtPutValue(XKEY.encodeToByteArray(), XVAL.encodeToByteArray()) }
                runCatching { t.gossipPublish(TOPIC, "gossip-hi".encodeToByteArray()) }
                delay(2000)
            }
        }
        Log.i(TAG, "SERVER ready")
    }

    /**
     * NSD SERVER role: advertises this libp2p host on the LAN via Android
     * NsdManager (the working LAN-discovery path on Android). Echoes pings, seeds
     * a DHT value, publishes gossip. Stays alive.
     */
    suspend fun runNsdServer(context: Context, port: Int) {
        val t = Libp2pTransport(Libp2pConfig(seed(), listenAddrs = listOf("/ip4/0.0.0.0/tcp/$port")))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        t.start()
        Log.i(TAG, "NSD-SERVER peerId=${t.peerId} port=$port ip=${DeviceNet.localIpv4(context)}")
        t.registerStreamHandler(PING, object : StreamHandler {
            override suspend fun handle(stream: Libp2pStream) {
                val req = stream.readBytes()
                stream.writeBytes(req)
                stream.close()
            }
        })
        t.gossipSubscribe(TOPIC).launchIn(scope)
        scope.launch {
            while (true) {
                runCatching { t.dhtPutValue(XKEY.encodeToByteArray(), XVAL.encodeToByteArray()) }
                runCatching { t.gossipPublish(TOPIC, "gossip-hi".encodeToByteArray()) }
                delay(2000)
            }
        }
        NsdDiscovery(context, t.peerId) { _, _ -> }.register(port)
        Log.i(TAG, "NSD-SERVER ready (advertised via NsdManager)")
    }

    /**
     * NSD CLIENT role: discovers the server via Android NsdManager (no peerId
     * passed in — learned from the service's TXT record), dials the resolved
     * multiaddr, then ping + DHT + gossip. Proves LAN auto-discovery on Android.
     */
    suspend fun runNsdClient(context: Context) {
        val t = Libp2pTransport(Libp2pConfig(seed(), listenAddrs = listOf("/ip4/0.0.0.0/tcp/0")))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val discovered = CompletableDeferred<String>()
        var nsd: NsdDiscovery? = null
        try {
            t.start()
            Log.i(TAG, "NSD-CLIENT peerId=${t.peerId}; discovering via NsdManager")
            nsd = NsdDiscovery(context, t.peerId) { maddr, peerId ->
                Log.i(TAG, "NSD discovered $maddr")
                scope.launch {
                    runCatching { t.connect(maddr) }
                        .onSuccess { if (!discovered.isCompleted) discovered.complete(peerId) }
                        .onFailure { Log.w(TAG, "NSD connect failed: ${it.message}") }
                }
            }
            nsd.discover()
            val serverPeer = withTimeoutOrNull(40000) { discovered.await() }
            if (serverPeer == null) {
                Log.i(TAG, "NSD PING=FAIL (no peer discovered/connected within 40s)")
            } else {
                Log.i(TAG, "NSD connected to $serverPeer")
                val s = t.openStream(serverPeer, PING)
                s.writeBytes("hello".encodeToByteArray())
                val echo = s.readBytes().decodeToString()
                s.close()
                Log.i(TAG, "NSD PING=${if (echo == "hello") "PASS" else "FAIL"} echo='$echo'")
                delay(2000)
                runCatching {
                    val got = t.dhtGetValues(XKEY.encodeToByteArray(), 1)
                    val ok = got.isNotEmpty() && got[0].decodeToString() == XVAL
                    Log.i(TAG, "NSD DHT=${if (ok) "PASS" else "FAIL"}")
                }.onFailure { Log.w(TAG, "NSD DHT=ERROR ${it.message}") }
                val received = CompletableDeferred<String>()
                t.gossipSubscribe(TOPIC)
                    .onEach { if (!received.isCompleted) received.complete(it.payload.decodeToString()) }
                    .launchIn(scope)
                val msg = withTimeoutOrNull(15000) { received.await() }
                Log.i(TAG, "NSD GOSSIP=${if (msg == "gossip-hi") "PASS" else "FAIL"} got='$msg'")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "NSD ERROR ${e.message}", e)
        } finally {
            nsd?.stop()
            scope.cancel()
            runCatching { t.stop() }
            Log.i(TAG, "NSD DONE")
        }
    }

    /**
     * mDNS CLIENT role (emulator B, requires emulator 36.5+ shared virtual Wi-Fi).
     * Does NOT dial: relies on go-libp2p mDNS auto-discovery + auto-connect. Proof
     * of discovery is openStream([remotePeerId]) succeeding without an explicit
     * connect(). Then DHT get + gossip. Read with `adb -s emulator-5556 logcat -s P2pSmoke`.
     */
    suspend fun runClientMdns(remotePeerId: String) {
        val t = Libp2pTransport(Libp2pConfig(seed(), listenAddrs = listOf("/ip4/0.0.0.0/tcp/0")))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            t.start()
            Log.i(TAG, "MDNS-CLIENT peerId=${t.peerId}; awaiting mDNS discovery of $remotePeerId (no explicit dial)")
            var echo = ""
            var ok = false
            repeat(15) {
                if (!ok) {
                    runCatching {
                        val s = t.openStream(remotePeerId, PING)
                        s.writeBytes("hello".encodeToByteArray())
                        echo = s.readBytes().decodeToString()
                        s.close()
                        if (echo == "hello") ok = true
                    }
                    if (!ok) delay(2000)
                }
            }
            Log.i(TAG, "MDNS PING=${if (ok) "PASS" else "FAIL"} echo='$echo' (discovery via mDNS, no dial)")
            if (ok) {
                runCatching {
                    val got = t.dhtGetValues(XKEY.encodeToByteArray(), 1)
                    val dhtOk = got.isNotEmpty() && got[0].decodeToString() == XVAL
                    Log.i(TAG, "MDNS DHT=${if (dhtOk) "PASS" else "FAIL"}")
                }.onFailure { Log.w(TAG, "MDNS DHT=ERROR ${it.message}") }
                val received = CompletableDeferred<String>()
                t.gossipSubscribe(TOPIC)
                    .onEach { if (!received.isCompleted) received.complete(it.payload.decodeToString()) }
                    .launchIn(scope)
                val msg = withTimeoutOrNull(15000) { received.await() }
                Log.i(TAG, "MDNS GOSSIP=${if (msg == "gossip-hi") "PASS" else "FAIL"} got='$msg'")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "MDNS ERROR ${e.message}", e)
        } finally {
            scope.cancel()
            runCatching { t.stop() }
            Log.i(TAG, "MDNS DONE")
        }
    }

    /**
     * Cross-process CLIENT role (emulator B). Dials [remote] (a full multiaddr),
     * then runs ping + DHT get + gossip receive, logging PASS/FAIL. Read with
     * `adb -s emulator-5556 logcat -s P2pSmoke`.
     */
    suspend fun runClient(remote: String) {
        val t = Libp2pTransport(Libp2pConfig(seed(), listenAddrs = listOf("/ip4/0.0.0.0/tcp/0")))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            t.start()
            Log.i(TAG, "CLIENT peerId=${t.peerId}")
            t.connect(remote)
            Log.i(TAG, "CLIENT connected to $remote")
            val remotePeer = remote.substringAfterLast("/p2p/")

            val s = t.openStream(remotePeer, PING)
            s.writeBytes("hello".encodeToByteArray())
            val echo = s.readBytes().decodeToString()
            s.close()
            Log.i(TAG, "XPROC PING=${if (echo == "hello") "PASS" else "FAIL"} echo='$echo'")

            delay(2500)
            var dhtOk = false
            repeat(6) {
                if (!dhtOk) {
                    runCatching {
                        val got = t.dhtGetValues(XKEY.encodeToByteArray(), 1)
                        if (got.isNotEmpty() && got[0].decodeToString() == XVAL) dhtOk = true
                    }
                    if (!dhtOk) delay(1500)
                }
            }
            Log.i(TAG, "XPROC DHT=${if (dhtOk) "PASS" else "FAIL"}")

            val received = CompletableDeferred<String>()
            t.gossipSubscribe(TOPIC)
                .onEach { if (!received.isCompleted) received.complete(it.payload.decodeToString()) }
                .launchIn(scope)
            val msg = withTimeoutOrNull(15000) { received.await() }
            Log.i(TAG, "XPROC GOSSIP=${if (msg == "gossip-hi") "PASS" else "FAIL"} got='$msg'")
        } catch (e: Throwable) {
            Log.e(TAG, "XPROC ERROR ${e.message}", e)
        } finally {
            scope.cancel()
            runCatching { t.stop() }
            Log.i(TAG, "XPROC DONE")
        }
    }

    /**
     * Debug account seeder for the Phase C discovery checkpoint. Idempotent and
     * gap-filling: creates an identity only if none exists, sets PIN 1234 only if
     * unset, and stores a minimal profile only if none exists — it never
     * overwrites an existing account. Needed because announcing presence requires
     * a local profile (its CID), which `restoreIdentity` does not recreate.
     * Logs the recovery phrase (debug only) so it can be recorded in test-accounts.md.
     */
    suspend fun runSeedAccount(
        authRepository: AuthRepository,
        repositoryManager: RepositoryManager,
        displayName: String,
        age: Int,
        interests: List<String>,
    ) {
        try {
            if (!authRepository.hasIdentity()) {
                val phrase = authRepository.generateIdentity().getOrThrow()
                Log.i(TAG, "SEED generated identity; phrase=${phrase.toDisplayString()}")
            } else {
                Log.i(TAG, "SEED identity already present")
            }
            if (!authRepository.hasPin()) authRepository.setPin("1234").getOrThrow()
            if (repositoryManager.getMyProfileCid() == null) {
                val identity = authRepository.getIdentity() ?: error("no identity after generate")
                val profile = UserProfile(
                    did = identity.did,
                    displayName = displayName,
                    bio = "Phase C discovery test account",
                    age = age,
                    interests = interests,
                    signingKey = identity.publicKey,
                    createdAt = kotlin.time.Clock.System.now().toString(),
                )
                repositoryManager.putProfile(profile).getOrThrow()
            } else if (repositoryManager.getMyProfileEnvelope() == null) {
                // Profile predates Phase D per-record signing (signature is NULL):
                // re-save the same record so it gets a per-record signature.
                val existing = repositoryManager.getMyProfile()
                if (existing != null) {
                    repositoryManager.putProfile(existing).getOrThrow()
                    Log.i(TAG, "SEED re-signed existing profile")
                }
            }
            Log.i(
                TAG,
                "SEED DONE did=${authRepository.getDid()} profileCid=${repositoryManager.getMyProfileCid()} name=$displayName",
            )
        } catch (e: Throwable) {
            Log.e(TAG, "SEED ERROR ${e.message}", e)
        }
    }

    /**
     * Phase G.2 FEEDCHECK role (run on BOTH emulators). Exercises the production
     * `PeerProfileFeed` facade end to end - the exact code the Home screen calls:
     * ensureStarted() (host + serving handler + NSD LAN bootstrap + presence
     * heartbeat) then candidates() (discover -> fetch -> verify -> decode). Logs
     * each resolved profile card. PASS = each emulator logs "FEED card" for the
     * other's real profile. A debug geohash forces both into the same cell so the
     * result doesn't depend on emulator location. `adb logcat -s P2pSmoke`.
     */
    suspend fun runFeedCheck(feed: PeerProfileFeed, locator: GeohashLocator, geohash: String) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val seen = mutableSetOf<String>()
        try {
            locator.setDebugGeohash(geohash)
            Log.i(TAG, "FEED starting (geohash=$geohash)")
            scope.launch {
                feed.candidates().collect { candidate ->
                    val profile = candidate.profile
                    if (seen.add(profile.did)) {
                        Log.i(TAG, "FEED card did=${profile.did} name='${profile.displayName}' bio='${profile.bio}' age=${profile.age}")
                    }
                }
            }
            delay(90_000)
            Log.i(TAG, "FEED cards=${seen.size}")
        } catch (e: Throwable) {
            Log.e(TAG, "FEED ERROR ${e.message}", e)
        } finally {
            scope.cancel()
            Log.i(TAG, "FEED DONE (cards=${seen.size})")
        }
    }

    /**
     * Phase D PROFILEFETCH role (run on BOTH emulators). Discovers peers via Phase
     * C presence (shared debug geohash, NSD LAN bootstrap), then for each verified
     * candidate fetches its **full profile** over the direct-P2P cascade
     * (/datingapp/profile/1.0.0), verifies the owner signature, decodes, and logs
     * displayName/bio/age. Also serves its own profile (registers the handler) and
     * checks that a second fetch is a local-cache hit. PASS = each emulator logs
     * "FETCH OK" for the other's profile. `adb logcat -s P2pSmoke`.
     */
    suspend fun runProfileFetch(
        context: Context,
        transport: Libp2pTransport,
        discovery: DiscoveryService,
        fetcher: ProfileFetcher,
        streamServer: StreamProfileFetcher,
        locator: GeohashLocator,
        peerDirectory: fyp.project.datingapp.p2p.discovery.PeerDirectory,
        geohash: String,
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var nsd: NsdDiscovery? = null
        val fetched = mutableSetOf<String>()
        try {
            locator.setDebugGeohash(geohash)
            transport.start()
            streamServer.register(transport) // serve our own profile on the fetch protocol
            val port = PresenceAnnouncer.parseTcpPort(transport.listenAddrs) ?: 4001
            Log.i(TAG, "FETCH peerId=${transport.peerId} port=$port geohash=$geohash ip=${DeviceNet.localIpv4(context)}")

            nsd = NsdDiscovery(context, transport.peerId) { maddr, _ ->
                scope.launch { runCatching { transport.connect(maddr) } }
            }
            nsd.register(port)
            nsd.discover()

            scope.launch {
                while (isActive) {
                    runCatching { discovery.announceOnce() }
                    delay(5000)
                }
            }

            scope.launch {
                discovery.candidates().collect { rec ->
                    if (fetched.add(rec.did)) {
                        Log.i(TAG, "FETCH discovered ${rec.did} cid=${rec.profileCid}; fetching profile")
                        // Fetch in its own coroutine so the candidates collector keeps
                        // consuming (and keeps PeerDirectory fresh) while we retry.
                        scope.launch {
                            var profile: fyp.project.datingapp.records.UserProfile? = null
                            repeat(10) { attempt ->
                                if (profile == null) {
                                    val contact = peerDirectory.get(rec.did)
                                    val result = runCatching { fetcher.fetch(rec.did, rec.profileCid) }
                                    profile = result.getOrNull()
                                    if (profile == null) {
                                        Log.i(TAG, "FETCH try=$attempt did=${rec.did} peer=${contact?.peerId} maddr=${contact?.multiaddrs} err=${result.exceptionOrNull()?.message}")
                                        delay(4000)
                                    }
                                }
                            }
                            val p = profile
                            if (p != null) {
                                Log.i(TAG, "FETCH OK did=${rec.did} name='${p.displayName}' bio='${p.bio}' age=${p.age}")
                                val again = runCatching { fetcher.fetch(rec.did, rec.profileCid) }.getOrNull()
                                Log.i(TAG, "FETCH cache-hit-second=${if (again != null) "OK" else "MISS"}")
                            } else {
                                Log.i(TAG, "FETCH FAIL did=${rec.did} (no verified profile after retries)")
                            }
                        }
                    }
                }
            }

            delay(90_000)
            Log.i(TAG, "FETCH fetched=${fetched.size}")
        } catch (e: Throwable) {
            Log.e(TAG, "FETCH ERROR ${e.message}", e)
        } finally {
            nsd?.stop()
            discovery.stopAnnouncing()
            scope.cancel()
            Log.i(TAG, "FETCH DONE (count=${fetched.size})")
        }
    }

    /**
     * Phase C DISCOVERY role (run on BOTH emulators). Uses the app's real Koin
     * singletons (shared transport, announcer, DiscoveryService) plus the real
     * signing identity + profile of the account installed on this emulator.
     * Bootstraps the two hosts over the LAN via NsdManager (DHT/GossipSub need a
     * connection first), forces a shared debug geohash on both, announces self in
     * a loop, and logs every verified candidate it discovers. PASS = each
     * emulator logs "DISCOVERY FOUND" for the other's DID/peerId/profileCid.
     * Optional age filter (--ei p2p_age_min/p2p_age_max) narrows results.
     * Read with `adb logcat -s P2pSmoke`.
     */
    suspend fun runDiscovery(
        context: Context,
        transport: Libp2pTransport,
        discovery: DiscoveryService,
        locator: GeohashLocator,
        geohash: String,
        filterAgeMin: Int?,
        filterAgeMax: Int?,
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var nsd: NsdDiscovery? = null
        val seen = mutableSetOf<String>()
        try {
            locator.setDebugGeohash(geohash)
            transport.start()
            val port = PresenceAnnouncer.parseTcpPort(transport.listenAddrs) ?: 4001
            Log.i(TAG, "DISCOVERY peerId=${transport.peerId} port=$port geohash=$geohash ip=${DeviceNet.localIpv4(context)}")

            // LAN bootstrap: find the other emulator and dial it so the DHT/mesh forms.
            nsd = NsdDiscovery(context, transport.peerId) { maddr, _ ->
                Log.i(TAG, "DISCOVERY NSD found $maddr")
                scope.launch {
                    runCatching { transport.connect(maddr) }
                        .onFailure { Log.w(TAG, "DISCOVERY connect failed: ${it.message}") }
                }
            }
            nsd.register(port)
            nsd.discover()

            // Announce loop (a DHT put with no connected peer fails, so retry).
            scope.launch {
                while (isActive) {
                    val ok = runCatching { discovery.announceOnce() }.getOrDefault(false)
                    Log.i(TAG, "DISCOVERY announceOnce=${if (ok) "PUBLISHED" else "SKIPPED(missing prereq)"}")
                    delay(5000)
                }
            }

            // Collect candidates.
            val filters = if (filterAgeMin != null && filterAgeMax != null) {
                DiscoveryFilters(ageRange = AgeRange(filterAgeMin, filterAgeMax))
            } else {
                DiscoveryFilters()
            }
            Log.i(TAG, "DISCOVERY collecting candidates filters=$filters")
            scope.launch {
                discovery.candidates(filters).collect { rec ->
                    if (seen.add(rec.did)) {
                        Log.i(
                            TAG,
                            "DISCOVERY FOUND did=${rec.did} peerId=${rec.peerId} cid=${rec.profileCid} " +
                                "geohash=${rec.geohash} maddrs=${rec.multiaddrs} announcedAt=${rec.announcedAt}",
                        )
                    }
                }
            }

            delay(90_000)
            Log.i(TAG, "DISCOVERY unique candidates=${seen.size}")
        } catch (e: Throwable) {
            Log.e(TAG, "DISCOVERY ERROR ${e.message}", e)
        } finally {
            nsd?.stop()
            discovery.stopAnnouncing()
            scope.cancel()
            // Leave the shared transport running (Koin singleton); only our loops stop.
            Log.i(TAG, "DISCOVERY DONE (found=${seen.size})")
        }
    }

    /**
     * Opt-in BLE proximity role (physical devices — emulators have no BLE radio).
     * Advertises [beacon] and scans for the same app service UUID, logging each
     * nearby device. Run on two phones with this role; PASS = each logs "BLE FOUND"
     * for the other. Read with `adb logcat -s P2pSmoke`.
     */
    suspend fun runBle(ble: BleProximity, beacon: BleBeacon) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val seen = mutableSetOf<String>()
        try {
            Log.i(TAG, "BLE supported=${ble.isSupported}; advertising $beacon and scanning")
            scope.launch {
                ble.start(beacon).collect { s ->
                    if (seen.add(s.deviceAddress)) {
                        Log.i(TAG, "BLE FOUND addr=${s.deviceAddress} did=${s.didSuffix} peer=${s.peerIdSuffix} rssi=${s.rssi}")
                    }
                }
            }
            delay(60_000)
            Log.i(TAG, "BLE unique devices=${seen.size}")
        } catch (e: Throwable) {
            Log.e(TAG, "BLE ERROR ${e.message}", e)
        } finally {
            ble.stop()
            scope.cancel()
            Log.i(TAG, "BLE DONE (found=${seen.size})")
        }
    }

    suspend fun run() {
        val a = Libp2pTransport(Libp2pConfig(seed(), listenAddrs = listOf("/ip4/127.0.0.1/tcp/0")))
        val b = Libp2pTransport(Libp2pConfig(seed(), listenAddrs = listOf("/ip4/127.0.0.1/tcp/0")))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            a.start()
            b.start()
            Log.i(TAG, "B3 host A: peerId=${a.peerId} addrs=${a.listenAddrs}")
            Log.i(TAG, "B3 host B: peerId=${b.peerId} addrs=${b.listenAddrs}")

            // ---- ping round-trip ----
            a.registerStreamHandler(PING, object : StreamHandler {
                override suspend fun handle(stream: Libp2pStream) {
                    val req = stream.readBytes()
                    stream.writeBytes(req) // echo
                    stream.close()
                }
            })
            val aAddr = a.listenAddrs.first() + "/p2p/" + a.peerId
            b.connect(aAddr)
            Log.i(TAG, "B dialled A ($aAddr)")

            val s = b.openStream(a.peerId, PING)
            s.writeBytes("hello".encodeToByteArray())
            val echo = s.readBytes().decodeToString()
            s.close()
            Log.i(TAG, "PING result=${if (echo == "hello") "PASS" else "FAIL"} echo='$echo'")

            // give identify + DHT routing tables a moment to populate
            delay(2000)

            // ---- DHT put/get ----
            try {
                val key = "smoke-key".encodeToByteArray()
                val value = "smoke-value".encodeToByteArray()
                a.dhtPutValue(key, value)
                delay(800)
                val got = b.dhtGetValues(key, 1)
                val ok = got.isNotEmpty() && got[0].contentEquals(value)
                Log.i(TAG, "DHT result=${if (ok) "PASS" else "FAIL"} results=${got.size}")
            } catch (e: Exception) {
                Log.w(TAG, "DHT result=ERROR (2-node DHT can be flaky): ${e.message}")
            }

            // ---- GossipSub pub/sub ----
            try {
                val received = CompletableDeferred<String>()
                a.gossipSubscribe(TOPIC)
                    .onEach { if (!received.isCompleted) received.complete(it.payload.decodeToString()) }
                    .launchIn(scope)
                b.gossipSubscribe(TOPIC).launchIn(scope) // B joins the mesh too (not just fanout)
                delay(4000) // mesh GRAFT over a few GossipSub heartbeats
                b.gossipPublish(TOPIC, "gossip-hi".encodeToByteArray())
                val msg = withTimeoutOrNull(8000) { received.await() }
                Log.i(TAG, "GOSSIP result=${if (msg == "gossip-hi") "PASS" else "FAIL"} got='$msg'")
            } catch (e: Exception) {
                Log.w(TAG, "GOSSIP result=ERROR: ${e.message}")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "SMOKE ERROR: ${e.message}", e)
        } finally {
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            runCatching { a.stop() }
            runCatching { b.stop() }
            Log.i(TAG, "SMOKE DONE")
        }
    }

    /**
     * MESSAGECHECK (run on BOTH emulators, each with the other's DID as p2p_target_did).
     * Brings the production feed up, drives discovery so the peer enters the PeerDirectory,
     * sends one message, and logs delivery-state transitions (DELIVERED on a direct ack;
     * SENT -> DELIVERED once a reverse receipt arrives) plus any messages received from the
     * peer. PASS = each side logs an outgoing row reaching DELIVERED and an incoming row.
     */
    suspend fun runMessageCheck(
        feed: PeerProfileFeed,
        messageService: MessageService,
        messageDao: MessageDao,
        peerDirectory: PeerDirectory,
        locator: GeohashLocator,
        geohash: String,
        targetDid: String,
        text: String,
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            locator.setDebugGeohash(geohash)
            feed.ensureStarted()
            Log.i(TAG, "MSG starting target=$targetDid geohash=$geohash")
            scope.launch {
                feed.candidates().collect { Log.i(TAG, "MSG feed card did=${it.profile.did} name='${it.profile.displayName}'") }
            }
            scope.launch {
                messageDao.getMessages(targetDid).collect { msgs ->
                    msgs.lastOrNull()?.let { Log.i(TAG, "MSG row dir=${it.direction} state=${it.deliveryState} text='${it.plaintext}'") }
                }
            }
            var sent = false
            repeat(40) {
                if (!sent && peerDirectory.get(targetDid) != null) {
                    val delivered = runCatching { messageService.sendMessage(targetDid, text) }.getOrDefault(false)
                    Log.i(TAG, "MSG sent delivered=$delivered")
                    sent = true
                }
                if (!sent) delay(3000)
            }
            if (!sent) Log.i(TAG, "MSG target never entered directory (peer offline?)")
            delay(90_000) // observe the reverse receipt + any incoming message
        } catch (e: Throwable) {
            Log.e(TAG, "MSG ERROR ${e.message}", e)
        } finally {
            scope.cancel()
            Log.i(TAG, "MSG DONE")
        }
    }

    /**
     * MATCHCHECK / LIKECHECK (run on BOTH emulators with the other's DID). Likes the target;
     * the second (reciprocal) like creates the match, marks the incoming like matched, and
     * seeds a conversation. PASS = the reciprocal side logs matched=true and a conversation.
     */
    suspend fun runMatchCheck(
        feed: PeerProfileFeed,
        likeService: LikeService,
        messageDao: MessageDao,
        peerDirectory: PeerDirectory,
        locator: GeohashLocator,
        geohash: String,
        targetDid: String,
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            locator.setDebugGeohash(geohash)
            feed.ensureStarted()
            Log.i(TAG, "MATCH starting target=$targetDid geohash=$geohash")
            scope.launch {
                feed.candidates().collect { Log.i(TAG, "MATCH feed card did=${it.profile.did} name='${it.profile.displayName}'") }
            }
            scope.launch {
                messageDao.getActiveConversations().collect { convos ->
                    Log.i(TAG, "MATCH conversations=${convos.map { it.peerDisplayName }}")
                }
            }
            var liked = false
            repeat(40) {
                if (!liked && peerDirectory.get(targetDid) != null) {
                    val matched = runCatching { likeService.sendLike(targetDid) }.getOrDefault(false)
                    Log.i(TAG, "MATCH like sent matched=$matched")
                    liked = true
                }
                if (!liked) delay(3000)
            }
            if (!liked) Log.i(TAG, "MATCH target never entered directory")
            delay(60_000)
        } catch (e: Throwable) {
            Log.e(TAG, "MATCH ERROR ${e.message}", e)
        } finally {
            scope.cancel()
            Log.i(TAG, "MATCH DONE")
        }
    }

    /**
     * INVALIDATECHECK (run on BOTH emulators). Logs each verified peer card, then after a
     * delay re-saves its own profile with a new bio, which publishes a Phase-E GossipSub
     * invalidation. PASS = the peer logs an updated card carrying the new bio.
     */
    suspend fun runInvalidateCheck(
        feed: PeerProfileFeed,
        repositoryManager: RepositoryManager,
        locator: GeohashLocator,
        geohash: String,
        newBio: String,
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            locator.setDebugGeohash(geohash)
            feed.ensureStarted()
            Log.i(TAG, "INVALIDATE starting geohash=$geohash")
            scope.launch {
                feed.candidates().collect { Log.i(TAG, "INVALIDATE card did=${it.profile.did} bio='${it.profile.bio}'") }
            }
            delay(30_000)
            val mine = runCatching { repositoryManager.getMyProfile() }.getOrNull()
            if (mine != null) {
                runCatching { repositoryManager.putProfile(mine.copy(bio = newBio)) }
                Log.i(TAG, "INVALIDATE published own profile update bio='$newBio'")
            } else {
                Log.i(TAG, "INVALIDATE no local profile to update")
            }
            delay(60_000) // peers should log an updated card with the new bio
        } catch (e: Throwable) {
            Log.e(TAG, "INVALIDATE ERROR ${e.message}", e)
        } finally {
            scope.cancel()
            Log.i(TAG, "INVALIDATE DONE")
        }
    }

    /**
     * HOLDERCHECK (Phase F cache-holder fallback). Repeatedly fetches [targetDid]'s profile
     * over the cascade; once the origin is offline the answer should still resolve from a
     * cacheHolder. **Needs a 3rd peer** (origin + holder + this fetcher) for a clean test;
     * with two emulators it just exercises the cascade. PASS = resolved=true after origin off.
     */
    suspend fun runHolderCheck(
        feed: PeerProfileFeed,
        fetcher: ProfileFetcher,
        locator: GeohashLocator,
        geohash: String,
        targetDid: String,
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            locator.setDebugGeohash(geohash)
            feed.ensureStarted()
            Log.i(TAG, "HOLDER starting target=$targetDid geohash=$geohash")
            scope.launch { feed.candidates().collect { /* drive discovery so a holder is reachable */ } }
            repeat(20) {
                val p = runCatching { fetcher.fetch(targetDid) }.getOrNull()
                Log.i(TAG, "HOLDER fetch did=$targetDid resolved=${p != null} name='${p?.displayName}'")
                delay(5000)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "HOLDER ERROR ${e.message}", e)
        } finally {
            scope.cancel()
            Log.i(TAG, "HOLDER DONE")
        }
    }

    /**
     * MAILBOXCHECK (persistent offline mailbox pull). Brings the feed up and repeatedly pulls
     * this device's queued mail from its holders (authenticated pull), logging the delivered
     * count; a deposit -> kill -> restart -> pull -> decrypt run proves persistence. **Needs a
     * holder distinct from sender + recipient** (3 peers) for a clean offline test; the
     * in-process MailboxPullAuthTest covers the auth-pull contract.
     */
    suspend fun runMailboxCheck(
        feed: PeerProfileFeed,
        mailboxService: MailboxService,
        locator: GeohashLocator,
        geohash: String,
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            locator.setDebugGeohash(geohash)
            feed.ensureStarted()
            Log.i(TAG, "MAILBOX starting (pull own mail) geohash=$geohash")
            scope.launch { feed.candidates().collect { /* keep host + discovery alive */ } }
            repeat(12) {
                val n = runCatching { mailboxService.pullOwnMail() }.getOrDefault(0)
                Log.i(TAG, "MAILBOX pulled=$n")
                delay(10_000)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "MAILBOX ERROR ${e.message}", e)
        } finally {
            scope.cancel()
            Log.i(TAG, "MAILBOX DONE")
        }
    }
}
