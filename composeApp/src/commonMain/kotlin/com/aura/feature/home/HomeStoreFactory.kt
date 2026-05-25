package com.aura.feature.home

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.aura.database.RepositoryManager
import com.aura.domain.location.LocationProvider
import com.aura.feature.home.HomeStore.Intent
import com.aura.feature.home.HomeStore.Label
import com.aura.feature.home.HomeStore.State
import com.aura.p2p.discovery.FeedFilterPrefs
import com.aura.p2p.discovery.FeedFilterStore
import com.aura.p2p.feed.PeerProfileFeed
import com.aura.p2p.like.LikeService
import com.aura.p2p.messaging.MessageService
import com.aura.p2p.relay.RelayPolicy
import com.aura.p2p.relay.SessionInteractionTokens
import com.aura.p2p.transport.wire.SignedEnvelope
import com.aura.records.Match
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Clock

class HomeStoreFactory (
    private val storeFactory: StoreFactory,
    private val database: Database,
    private val repositoryManager: RepositoryManager,
    private val peerProfileFeed: PeerProfileFeed,
    private val relayPolicy: RelayPolicy,
    private val sessionTokens: SessionInteractionTokens,
    private val likeService: LikeService,
    private val messageService: MessageService,
    private val feedFilterStore: FeedFilterStore,
    private val locationProvider: LocationProvider,
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
        data class PictureLoaded(val profileDid: String, val pictureRef: String, val filePath: String) : Msg()
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
        data class FiltersChanged(val filters: FeedFilterPrefs): Msg()
        data class FilterSheetToggled(val open: Boolean): Msg()
        data class SelfPhotoLoaded(val path: String?): Msg()
    }

    private inner class ExecutorImpl: CoroutineExecutor<Intent, Nothing, State, Msg, Label>() {

        // Side-channel: the signed envelope each candidate was fetched from, keyed
        // by DID. Lives on the executor (not in MVI State) so raw ByteArrays never
        // enter the immutable state tree. Read by the relay-cache sighting hook.
        private val envelopeByDid = mutableMapOf<String, SignedEnvelope>()

        // The live feed collection; cancelled + relaunched whenever filters change so the stack
        // only ever holds profiles that match the current preferences.
        private var feedJob: Job? = null
        private var filtersLoaded = false

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
                Intent.OpenFilters -> dispatch(Msg.FilterSheetToggled(true))
                Intent.CloseFilters -> dispatch(Msg.FilterSheetToggled(false))
                is Intent.ApplyFilters -> applyFilters(intent.filters)
            }
        }

        private fun loadProfiles() {
            // Load persisted filters once on first start so the feed is filtered from the outset.
            if (!filtersLoaded) {
                dispatch(Msg.FiltersChanged(feedFilterStore.load()))
                filtersLoaded = true
            }
            loadSelfPhoto()
            restartFeed()
        }

        /** Resolve the user's own first photo path so the top-bar avatar shows it. */
        private fun loadSelfPhoto() {
            scope.launch {
                val ref = runCatching { repositoryManager.getMyProfile()?.photos?.firstOrNull()?.ref }.getOrNull()
                val path = ref?.let { runCatching { repositoryManager.blobFilePath(it) }.getOrNull() }
                dispatch(Msg.SelfPhotoLoaded(path))
            }
        }

        /** Persist the new filters, close the sheet, and restart the feed so the stack reflects them. */
        private fun applyFilters(filters: FeedFilterPrefs) {
            feedFilterStore.save(filters)
            dispatch(Msg.FiltersChanged(filters))
            dispatch(Msg.FilterSheetToggled(false))
            restartFeed()
        }

        /** (Re)start the candidate collection using the CURRENT [State.filters] + live location. */
        private fun restartFeed() {
            feedJob?.cancel()
            dispatch(Msg.ProfilesLoading) // clears the existing stack so non-matching cards disappear
            feedJob = scope.launch {
                try {
                    // Real peer feed (Phase G.2): discover peers, fetch + verify each full profile,
                    // and append it as a card as it arrives. Candidates are filtered by the user's
                    // persisted preferences (distance / age / gender / interests) PLUS the live
                    // coarse location for the distance check; non-matching profiles never surface.
                    val coords = if (state().filters.isDefault) null
                        else runCatching { locationProvider.currentCoarse() }.getOrNull()
                    val filters = state().filters.toDiscoveryFilters(coords)
                    peerProfileFeed.candidates(filters).collect { candidate ->
                        val profile = candidate.profile
                        // Stash the envelope for the sighting hook (off the MVI state).
                        envelopeByDid[profile.did] = candidate.envelope
                        val card = State.ProfileCardState(
                            profile = profile,
                            pictureBlobs = (profile.photos ?: emptyList()).map { blob ->
                                State.PictureState.Loading(blob.ref)
                            },
                        )
                        dispatch(Msg.ProfileArrived(card))
                        recordSighting(profile.did)
                        // Fetch each photo blob (Part 3); flips Loading -> Loaded as
                        // bytes land. Per-photo so one slow blob doesn't block others.
                        (profile.photos ?: emptyList()).forEach { blob ->
                            scope.launch { loadPicture(profile.did, blob.ref) }
                        }
                    }
                } catch (e: CancellationException) {
                    // A restart (filters changed) cancels the previous collection; that is
                    // not a feed error — let it unwind without clobbering the new Loading state.
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "Failed to load profiles"
                    dispatch(Msg.ProfilesError(message))
                    publish(Label.Error(message))
                }
            }
        }

        /**
         * Relay-cache sighting hook (`p2p-subsystem-design.md` §12.2). A card that
         * reaches the Home feed is treated as "sighted": mint a one-shot session
         * token and offer its signed envelope to [RelayPolicy]. The five defenses
         * inside [RelayPolicy.put] decide what actually lands; this never throws.
         *
         * TODO(Part 3 blobs): move the call site to the per-card render / picture-
         *   load path (Msg.PictureLoaded) so a sighting fires only when a card is
         *   genuinely on screen, and kick off blob fetch from the same hook.
         */
        private suspend fun recordSighting(profileDid: String) {
            val envelope = envelopeByDid[profileDid] ?: return
            val token = sessionTokens.issue()
            runCatching { relayPolicy.put(envelope, token) }
        }

        /**
         * Fetch one photo blob by CID (Part 3) and, on success, flip its card slot
         * Loading -> Loaded with the local file path. Failures (offline owner,
         * integrity mismatch, iOS stub) leave the slot Loading -> the card view keeps
         * showing the progress/placeholder. Never throws into the feed collector.
         */
        private suspend fun loadPicture(profileDid: String, pictureRef: String) {
            val path = runCatching { peerProfileFeed.loadPhoto(profileDid, pictureRef) }.getOrNull()
                ?: return
            dispatch(Msg.PictureLoaded(profileDid, pictureRef, path))
        }
        /**
         * Swipe-right (E): send a signed like to [id] and, if they had already liked us,
         * it becomes a match now -> raise the It's-a-match overlay. The conversation +
         * Match record are created inside [LikeService].
         */
        private fun profileLiked(id: String) {
            scope.launch {
                val card = loadedCard(id)
                val matched = runCatching { likeService.sendLike(id, card?.profile?.displayName) }.getOrDefault(false)
                if (matched) {
                    dispatch(
                        Msg.MatchOccured(
                            match = Match(subject = id, createdAt = Clock.System.now().toString()),
                            matchDialog = card?.profile?.displayName ?: id,
                            pictureBlobs = card?.pictureBlobs ?: emptyList(),
                        ),
                    )
                }
            }
        }

        private fun profileSwiped(id: String) { /* pass: no like sent; card removed via DismissProfile */ }

        /** First message from the It's-a-match overlay: deliver it, then close the overlay. */
        private fun sendMatchMessage(text: String) {
            val peerDid = state().dialog?.match?.subject
            scope.launch {
                if (!peerDid.isNullOrBlank() && text.isNotBlank()) {
                    runCatching { messageService.sendMessage(peerDid, text) }
                }
                dispatch(Msg.MatchDialogDismissed)
            }
        }

        private fun loadedCard(did: String): State.ProfileCardState? =
            (state().contentState as? State.ContentState.Loaded)?.profiles?.firstOrNull { it.profile.did == did }
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
                                                    State.PictureState.Loaded(msg.pictureRef, msg.filePath)
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
                is Msg.FiltersChanged -> copy(filters = msg.filters)
                is Msg.FilterSheetToggled -> copy(filterSheetOpen = msg.open)
                is Msg.SelfPhotoLoaded -> copy(selfPhotoPath = msg.path)
            }
    }

    interface Database {

    }

}
