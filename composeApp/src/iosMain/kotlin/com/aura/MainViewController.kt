package com.aura

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.aura.database.RepositoryManager
import com.aura.di.appModule
import com.aura.di.iosModule
import com.aura.domain.auth.AuthRepository
import com.aura.navigation.DefaultRootComponent
import com.aura.p2p.feed.PeerProfileFeed
import com.aura.p2p.relay.RelayPolicy
import com.aura.p2p.relay.SessionInteractionTokens
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
        likeService = koin.get(),
        photoUploader = koin.get(),
        backgroundService = koin.get(),
        incomingLikesDao = koin.get(),
        profileFetcher = koin.get(),
        feedFilterStore = koin.get(),
        locationProvider = koin.get(),
    )

    App(root)
}
