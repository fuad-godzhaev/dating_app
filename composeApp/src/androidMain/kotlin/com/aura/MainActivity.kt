package com.aura

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.aura.database.RepositoryManager
import com.aura.domain.auth.AuthRepository
import com.aura.navigation.DefaultRootComponent
import com.aura.p2p.background.BackgroundService
import com.aura.p2p.ble.BleBeacon
import com.aura.p2p.ble.BleProximity
import com.aura.p2p.discovery.DiscoveryService
import com.aura.p2p.discovery.GeohashLocator
import com.aura.p2p.discovery.PeerDirectory
import com.aura.p2p.fetch.ProfileFetcher
import com.aura.p2p.fetch.StreamProfileFetcher
import com.aura.p2p.feed.PeerProfileFeed
import com.aura.p2p.relay.RelayPolicy
import com.aura.p2p.relay.SessionInteractionTokens
import com.aura.p2p.transport.Libp2pTransport
import com.aura.p2p.transport.P2pSmoke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Coarse location is requested at app open (ADR-0002 cold-start pre-warm) so a
        // geohash cell is ready for discovery. Denial degrades gracefully (no announce).
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQ_LOCATION)
        }
        // Notifications (Android 13+): background message + stay-online notifications.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
        }

        // Koin + KeyMigration + background scheduling are set up in AuraApp (Application),
        // so background workers/services have a DI graph even with no Activity.
        val koin = GlobalContext.get()

        // Opt-out "Stay online": on a normal foreground launch, start the quiet, slotted,
        // reachability-gated FGS if enabled (default). Foreground start respects Android 12+ FGS
        // rules; the service self-gates (reachability + slot), so this is safe outside the slot.
        // Skipped for the debug smoke-test intents below.
        if (intent?.getStringExtra("p2p_role") == null && intent?.getBooleanExtra("p2p_smoke", false) != true) {
            runCatching { koin.get<BackgroundService>().startStayOnlineIfEnabled() }
        }

        // Debug-only B5 transport smoke tests, gated by intent extras so they never
        // run on normal launches. In-process: --ez p2p_smoke true. Cross-process
        // (two emulators): --es p2p_role server|client [--ei p2p_port N] [--es p2p_remote <multiaddr>].
        when (intent?.getStringExtra("p2p_role")) {
            "server" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runServer(
                    intent.getIntExtra("p2p_port", 4001),
                    intent.getStringExtra("p2p_host") ?: "0.0.0.0",
                )
            }
            "client" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runClient(intent.getStringExtra("p2p_remote").orEmpty())
            }
            "client_mdns" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runClientMdns(intent.getStringExtra("p2p_peer").orEmpty())
            }
            "nsd_server" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runNsdServer(applicationContext, intent.getIntExtra("p2p_port", 4001))
            }
            "nsd_client" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runNsdClient(applicationContext)
            }
            "discovery" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runDiscovery(
                    applicationContext,
                    koin.get<Libp2pTransport>(),
                    koin.get<DiscoveryService>(),
                    koin.get<GeohashLocator>(),
                    intent.getStringExtra("p2p_geohash") ?: "gc7x3",
                    intent.getIntExtra("p2p_age_min", -1).takeIf { it >= 18 },
                    intent.getIntExtra("p2p_age_max", -1).takeIf { it >= 18 },
                )
            }
            "ble" -> {
                requestBlePermissions()
                CoroutineScope(Dispatchers.IO).launch {
                    P2pSmoke.runBle(
                        koin.get<BleProximity>(),
                        BleBeacon(
                            intent.getStringExtra("ble_did") ?: "did00001",
                            intent.getStringExtra("ble_peer") ?: "peer0001",
                        ),
                    )
                }
            }
            "seedaccount" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runSeedAccount(
                    koin.get<AuthRepository>(),
                    koin.get<RepositoryManager>(),
                    intent.getStringExtra("seed_name") ?: "TestPeer",
                    intent.getIntExtra("seed_age", 28),
                    (intent.getStringExtra("seed_interests") ?: "music,hiking").split(","),
                )
            }
            "profilefetch" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runProfileFetch(
                    applicationContext,
                    koin.get<Libp2pTransport>(),
                    koin.get<DiscoveryService>(),
                    koin.get<ProfileFetcher>(),
                    koin.get<StreamProfileFetcher>(),
                    koin.get<GeohashLocator>(),
                    koin.get<PeerDirectory>(),
                    intent.getStringExtra("p2p_geohash") ?: "gc7x3",
                )
            }
            "feedcheck" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runFeedCheck(
                    koin.get<PeerProfileFeed>(),
                    koin.get<GeohashLocator>(),
                    intent.getStringExtra("p2p_geohash") ?: "gc7x3",
                )
            }
            "keycheck" -> CoroutineScope(Dispatchers.IO).launch { P2pSmoke.keyCheck() }
            "recoverycheck" -> CoroutineScope(Dispatchers.IO).launch {
                val repo = koin.get<AuthRepository>()
                runCatching {
                    repo.deleteAccount()
                    val phrase = repo.generateIdentity().getOrThrow()
                    val did1 = repo.getDid()
                    repo.deleteAccount()                       // simulate fresh install
                    repo.restoreIdentity(phrase).getOrThrow() // re-enter the same phrase
                    val did2 = repo.getDid()
                    val ok = did1 != null && did1 == did2
                    android.util.Log.i("P2pSmoke", "RECOVERY restore-same-DID: ${if (ok) "PASS" else "FAIL"} did1=$did1 did2=$did2")
                }.onFailure { android.util.Log.e("P2pSmoke", "RECOVERY ERROR ${it.message}", it) }
            }
            "messagecheck" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runMessageCheck(
                    koin.get(), koin.get(), koin.get(), koin.get(), koin.get<GeohashLocator>(),
                    intent.getStringExtra("p2p_geohash") ?: "gc7x3",
                    intent.getStringExtra("p2p_target_did").orEmpty(),
                    intent.getStringExtra("p2p_text") ?: "hello from p2p smoke",
                )
            }
            "matchcheck", "likecheck" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runMatchCheck(
                    koin.get(), koin.get(), koin.get(), koin.get(), koin.get<GeohashLocator>(),
                    intent.getStringExtra("p2p_geohash") ?: "gc7x3",
                    intent.getStringExtra("p2p_target_did").orEmpty(),
                )
            }
            "invalidatecheck" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runInvalidateCheck(
                    koin.get(), koin.get(), koin.get<GeohashLocator>(),
                    intent.getStringExtra("p2p_geohash") ?: "gc7x3",
                    intent.getStringExtra("p2p_text") ?: "updated bio (invalidate check)",
                )
            }
            "holdercheck" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runHolderCheck(
                    koin.get(), koin.get<ProfileFetcher>(), koin.get<GeohashLocator>(),
                    intent.getStringExtra("p2p_geohash") ?: "gc7x3",
                    intent.getStringExtra("p2p_target_did").orEmpty(),
                )
            }
            "mailboxcheck" -> CoroutineScope(Dispatchers.IO).launch {
                P2pSmoke.runMailboxCheck(
                    koin.get(), koin.get(), koin.get<GeohashLocator>(),
                    intent.getStringExtra("p2p_geohash") ?: "gc7x3",
                )
            }
            else -> if (intent?.getBooleanExtra("p2p_smoke", false) == true) {
                CoroutineScope(Dispatchers.IO).launch { P2pSmoke.run() }
            }
        }

        val root = DefaultRootComponent(
            componentContext = defaultComponentContext(),
            authRepository = koin.get<AuthRepository>(),
            repositoryManager = koin.get<RepositoryManager>(),
            storeFactory = koin.get<StoreFactory>(),
            peerProfileFeed = koin.get<PeerProfileFeed>(),
            relayPolicy = koin.get<RelayPolicy>(),
            sessionTokens = koin.get<SessionInteractionTokens>(),
            messageService = koin.get(),
            messageDao = koin.get(),
            likeService = koin.get(),
            photoUploader = koin.get(),
            backgroundService = koin.get(),
            incomingLikesDao = koin.get(),
            profileFetcher = koin.get(),
            feedFilterStore = koin.get(),
            locationProvider = koin.get(),
        )

        setContent {
            App(root)
        }
    }

    /** Request the BLE runtime permissions (debug `ble` role only; opt-in feature). */
    private fun requestBlePermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), REQ_BLE)
    }

    private companion object {
        const val REQ_LOCATION = 1001
        const val REQ_BLE = 1002
        const val REQ_NOTIF = 1003
    }
}
