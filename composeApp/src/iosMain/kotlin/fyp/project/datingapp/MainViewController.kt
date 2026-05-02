package fyp.project.datingapp

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.mvikotlin.core.store.StoreFactory
import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.di.appModule
import fyp.project.datingapp.di.iosModule
import fyp.project.datingapp.domain.auth.AuthRepository
import fyp.project.datingapp.navigation.DefaultRootComponent
import fyp.project.datingapp.p2p.feed.PeerProfileFeed
import fyp.project.datingapp.p2p.relay.RelayPolicy
import fyp.project.datingapp.p2p.relay.SessionInteractionTokens
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    val lifecycle = LifecycleRegistry()
    val componentContext = DefaultComponentContext(lifecycle = lifecycle)

    val koin = startKoin {
        modules(iosModule, appModule)
    }.koin

    val root = DefaultRootComponent(
        componentContext = componentContext,
        authRepository = koin.get<AuthRepository>(),
        repositoryManager = koin.get<RepositoryManager>(),
        storeFactory = koin.get<StoreFactory>(),
        peerProfileFeed = koin.get<PeerProfileFeed>(),
        relayPolicy = koin.get<RelayPolicy>(),
        sessionTokens = koin.get<SessionInteractionTokens>(),
        messageService = koin.get(),
        messageDao = koin.get(),
    )

    App(root)
}
