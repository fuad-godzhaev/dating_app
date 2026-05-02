package fyp.project.datingapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.mvikotlin.core.store.StoreFactory
import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.di.androidModule
import fyp.project.datingapp.di.appModule
import fyp.project.datingapp.domain.auth.AuthRepository
import fyp.project.datingapp.domain.auth.KeyMigration
import fyp.project.datingapp.navigation.DefaultRootComponent
import fyp.project.datingapp.p2p.ble.BleBeacon
import fyp.project.datingapp.p2p.ble.BleProximity
import fyp.project.datingapp.p2p.discovery.DiscoveryService
import fyp.project.datingapp.p2p.discovery.GeohashLocator
import fyp.project.datingapp.p2p.discovery.PeerDirectory
import fyp.project.datingapp.p2p.fetch.ProfileFetcher
import fyp.project.datingapp.p2p.fetch.StreamProfileFetcher
import fyp.project.datingapp.p2p.feed.PeerProfileFeed
import fyp.project.datingapp.p2p.relay.RelayPolicy
import fyp.project.datingapp.p2p.relay.SessionInteractionTokens
import fyp.project.datingapp.p2p.transport.AndroidTransportEnv
import fyp.project.datingapp.p2p.transport.Libp2pTransport
import fyp.project.datingapp.p2p.transport.P2pSmoke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AndroidTransportEnv.appContext = applicationContext

        // Coarse location is requested at app open (ADR-0002 cold-start pre-warm) so a
        // geohash cell is ready for discovery. Denial degrades gracefully (no announce).
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQ_LOCATION)
        }

        val koin = startKoin {
            androidContext(this@MainActivity)
            modules(androidModule, appModule)
        }.koin

        // P-256 re-split rollout: drop any legacy non-P-256 identity before the graph reads it.
        runBlocking {
            KeyMigration(koin.get<AuthRepository>()).runIfNeeded()
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
    }
}
