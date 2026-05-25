package com.aura.feature.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.aura.database.RepositoryManager
import com.aura.domain.location.LocationProvider
import com.aura.p2p.discovery.FeedFilterPrefs
import com.aura.p2p.discovery.FeedFilterStore
import com.aura.p2p.feed.PeerProfileFeed
import com.aura.p2p.like.LikeService
import com.aura.p2p.messaging.MessageService
import com.aura.p2p.relay.RelayPolicy
import com.aura.p2p.relay.SessionInteractionTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Decompose component backing the Home screen. Owns a retained
 * [HomeStore] via `instanceKeeper.getStore { … }` so configuration
 * changes (Android rotate) don't reset the swipe stack.
 *
 * Navigation callbacks ([onNavigateToEditProfile] /
 * [onNavigateToMessages]) are intentionally no-ops for Phase H —
 * Edit-Profile / Matches screens don't exist yet. Wired through in
 * Phase G.2+.
 */
interface HomeComponent {
    val state: Value<HomeStore.State>
    val labels: Flow<HomeStore.Label>

    fun onLoadProfiles()
    fun onSwiped(card: HomeStore.State.ProfileCardState, isLike: Boolean)
    fun onDismissProfile()
    fun onDismissDialog()
    fun onSendMatchMessage(text: String)
    fun onNavigateToEditProfile()
    fun onNavigateToMessages()
    fun onReport(targetDid: String, targetName: String)
    fun onOpenFilters()
    fun onCloseFilters()
    fun onApplyFilters(filters: FeedFilterPrefs)
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultHomeComponent(
    componentContext: ComponentContext,
    storeFactory: StoreFactory,
    repositoryManager: RepositoryManager,
    peerProfileFeed: PeerProfileFeed,
    relayPolicy: RelayPolicy,
    sessionTokens: SessionInteractionTokens,
    likeService: LikeService,
    messageService: MessageService,
    feedFilterStore: FeedFilterStore,
    locationProvider: LocationProvider,
    private val navigateToEditProfile: () -> Unit = {},
    private val navigateToMessages: () -> Unit = {},
    private val navigateToReport: (String, String) -> Unit = { _, _ -> },
) : HomeComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        HomeStoreFactory(
            storeFactory = storeFactory,
            database = object : HomeStoreFactory.Database {},
            repositoryManager = repositoryManager,
            peerProfileFeed = peerProfileFeed,
            relayPolicy = relayPolicy,
            sessionTokens = sessionTokens,
            likeService = likeService,
            messageService = messageService,
            feedFilterStore = feedFilterStore,
            locationProvider = locationProvider,
        ).provide()
    }

    private val _state = MutableValue(store.state)
    override val state: Value<HomeStore.State> = _state
    override val labels: Flow<HomeStore.Label> = store.labels

    private val scope = coroutineScope(Dispatchers.Main)

    init {
        scope.launch { store.stateFlow.collect { _state.value = it } }
        store.accept(HomeStore.Intent.LoadProfiles)
    }

    override fun onLoadProfiles() = store.accept(HomeStore.Intent.LoadProfiles)

    override fun onSwiped(card: HomeStore.State.ProfileCardState, isLike: Boolean) {
        store.accept(
            if (isLike) HomeStore.Intent.ProfileLiked(card.profile.did)
            else HomeStore.Intent.ProfileSwiped(card.profile.did),
        )
    }

    override fun onDismissProfile() = store.accept(HomeStore.Intent.DismissProfile)
    override fun onDismissDialog() = store.accept(HomeStore.Intent.DismissDialog)
    override fun onSendMatchMessage(text: String) =
        store.accept(HomeStore.Intent.MatchOccured(text))

    override fun onNavigateToEditProfile() {
        store.accept(HomeStore.Intent.OpenedProfile)
        navigateToEditProfile()
    }

    override fun onNavigateToMessages() {
        store.accept(HomeStore.Intent.OpenedMessages)
        navigateToMessages()
    }

    override fun onReport(targetDid: String, targetName: String) = navigateToReport(targetDid, targetName)

    override fun onOpenFilters() = store.accept(HomeStore.Intent.OpenFilters)
    override fun onCloseFilters() = store.accept(HomeStore.Intent.CloseFilters)
    override fun onApplyFilters(filters: FeedFilterPrefs) = store.accept(HomeStore.Intent.ApplyFilters(filters))
}
