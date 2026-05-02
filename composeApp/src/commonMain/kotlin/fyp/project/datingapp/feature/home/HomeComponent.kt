package fyp.project.datingapp.feature.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.p2p.feed.PeerProfileFeed
import fyp.project.datingapp.p2p.relay.RelayPolicy
import fyp.project.datingapp.p2p.relay.SessionInteractionTokens
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
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultHomeComponent(
    componentContext: ComponentContext,
    storeFactory: StoreFactory,
    repositoryManager: RepositoryManager,
    peerProfileFeed: PeerProfileFeed,
    relayPolicy: RelayPolicy,
    sessionTokens: SessionInteractionTokens,
    private val navigateToEditProfile: () -> Unit = {},
    private val navigateToMessages: () -> Unit = {},
) : HomeComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        HomeStoreFactory(
            storeFactory = storeFactory,
            database = object : HomeStoreFactory.Database {},
            repositoryManager = repositoryManager,
            peerProfileFeed = peerProfileFeed,
            relayPolicy = relayPolicy,
            sessionTokens = sessionTokens,
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
}
