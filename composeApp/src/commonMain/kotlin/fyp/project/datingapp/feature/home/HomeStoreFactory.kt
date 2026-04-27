package fyp.project.datingapp.feature.home

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.feature.home.HomeStore.Intent
import fyp.project.datingapp.feature.home.HomeStore.Label
import fyp.project.datingapp.feature.home.HomeStore.State
import fyp.project.datingapp.p2p.feed.PeerProfileFeed
import fyp.project.datingapp.records.Match
import kotlinx.coroutines.launch

class HomeStoreFactory (
    private val storeFactory: StoreFactory,
    private val database: Database,
    private val repositoryManager: RepositoryManager,
    private val peerProfileFeed: PeerProfileFeed,
) {

    fun provide(): HomeStore =
        object: HomeStore, Store<Intent, State, Label> by storeFactory.create(
            name = "HomeStore",
            initialState = State(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImp
        ) {}

    private sealed class Msg {
        data object ProfilesLoading : Msg()
        data class ProfilesLoaded(val profiles: List<State.ProfileCardState>): Msg()
        data class ProfileArrived(val card: State.ProfileCardState): Msg()
        data class PictureLoaded(val profileDid: String, val pictureRef: String) : Msg()
        data object TopCardRemoved: Msg()
        data class MatchOccured(
            val match: Match,
            val matchDialog: String,
            val pictureBlobs: List<State.PictureState>,
        ): Msg()
        data object OpenedMessages: Msg()
        data object OpenedProfile: Msg()
        data class ProfilesError(val message: String): Msg()
        data object MatchDialogDismissed: Msg()
    }

    private inner class ExecutorImpl: CoroutineExecutor<Intent, Nothing, State, Msg, Label>() {

        override fun executeIntent(intent: Intent) {
            when (intent) {
                Intent.LoadProfiles -> loadProfiles()
                is Intent.ProfileLiked -> profileLiked(intent.id)
                is Intent.ProfileSwiped -> profileSwiped(intent.id)
                Intent.DismissProfile -> dispatch(Msg.TopCardRemoved)
                Intent.DismissDialog -> dispatch(Msg.MatchDialogDismissed)
                is Intent.MatchOccured -> sendMatchMessage(intent.text)
                Intent.OpenedMessages -> dispatch(Msg.OpenedMessages)
                Intent.OpenedProfile -> dispatch(Msg.OpenedProfile)
                is Intent.ProfileExtended -> { /* TODO: Phase G.2 */ }
            }
        }

        private fun loadProfiles() {
            dispatch(Msg.ProfilesLoading)
            scope.launch {
                try {
                    // Real peer feed (Phase G.2): discover peers, fetch + verify each
                    // full profile, and append it as a card as it arrives. The flow is
                    // continuous (GossipSub-backed), so collection runs for the store's
                    // lifetime. TODO(G.2 follow-up): photo-blob fetch + relay-cache
                    //   sighting hook on Msg.PictureLoaded; client-side DiscoveryFilters.
                    peerProfileFeed.candidates().collect { profile ->
                        val card = State.ProfileCardState(
                            profile = profile,
                            pictureBlobs = (profile.photos ?: emptyList()).map { blob ->
                                State.PictureState.Loading(blob.ref)
                            },
                        )
                        dispatch(Msg.ProfileArrived(card))
                    }
                } catch(e: Exception) {
                    val message = e.message ?: "Failed to load profiles"
                    dispatch(Msg.ProfilesError(message))
                    publish(Label.Error(message))
                }
            }
        }
        private suspend fun loadPicture(profileDid: String, pictureRef: String) {
            try {
                //TODO: getPicture
            } catch(_: Exception) {
                //TODO: pictureFailed msg
            }
        }
        private fun profileLiked(id: String) {}
        private fun profileSwiped(id: String) {}
        private fun sendMatchMessage(text: String) {}
    }


    private object ReducerImp: Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State =
            when (msg) {
                Msg.ProfilesLoading -> copy(contentState = State.ContentState.Loading)
                is Msg.ProfilesLoaded -> copy(contentState  = State.ContentState.Loaded(profiles = msg.profiles))
                is Msg.ProfileArrived -> {
                    val existing = (contentState as? State.ContentState.Loaded)?.profiles ?: emptyList()
                    if (existing.any { it.profile.did == msg.card.profile.did }) this
                    else copy(contentState = State.ContentState.Loaded(profiles = existing + msg.card))
                }
                is Msg.ProfilesError -> copy(contentState = State.ContentState.Error(msg.message))
                is Msg.PictureLoaded -> {
                    val content = contentState
                    if (content is State.ContentState.Loaded) {
                        copy(
                            contentState = content.copy(
                                profiles = content.profiles.map { card ->
                                    if (card.profile.did == msg.profileDid) {
                                        card.copy(
                                            pictureBlobs = card.pictureBlobs.map { pic ->
                                                if (pic is State.PictureState.Loading &&
                                                    pic.ref == msg.pictureRef
                                                    ) {
                                                    State.PictureState.Loaded(msg.pictureRef)
                                                } else pic
                                            }
                                        )
                                    } else card
                                }
                            )
                        )
                    } else this
                }
                Msg.TopCardRemoved -> {
                    val content = contentState
                    if (content is State.ContentState.Loaded) {
                        copy(contentState = content.copy(profiles=content.profiles.dropLast(1)))
                    } else this
                }
                is Msg.MatchOccured -> copy(
                    dialog = State.MatchDialogState(
                        match = msg.match,
                        matchDialog = msg.matchDialog,
                        pictureBlobs = msg.pictureBlobs,
                    )
                )
                Msg.MatchDialogDismissed -> copy(dialog = null)
                Msg.OpenedMessages -> this
                Msg.OpenedProfile -> this
            }
    }

    interface Database {

    }

}
