package com.aura.feature.profile

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnResume
import com.aura.database.RepositoryManager
import com.aura.domain.auth.AuthRepository
import com.aura.p2p.background.BackgroundService
import com.aura.p2p.feed.PeerProfileFeed
import com.aura.records.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Self profile overview (AURA_DESIGN_SPEC §6.10). */
interface ProfileOverviewComponent {
    val state: Value<State>
    fun onSettings()
    fun onEditProfile()
    fun onRecoveryPhrase()
    fun onSignOut()
    fun onBack()

    data class State(
        val profile: UserProfile? = null,
        val did: String? = null,
        // Local on-disk paths for the user's photos (resolved from blob refs) so the
        // strip shows real images instead of gradient placeholders.
        val photos: List<String> = emptyList(),
        val signingOut: Boolean = false,
    )
}

class DefaultProfileOverviewComponent(
    componentContext: ComponentContext,
    private val repositoryManager: RepositoryManager,
    private val authRepository: AuthRepository,
    private val peerProfileFeed: PeerProfileFeed,
    private val backgroundService: BackgroundService,
    private val onSettingsClick: () -> Unit,
    private val onEditProfileClick: () -> Unit,
    private val onRecoveryPhraseClick: () -> Unit = {},
    private val onSignedOut: () -> Unit,
    private val onBackClick: () -> Unit,
) : ProfileOverviewComponent, ComponentContext by componentContext {

    private val _state = MutableValue(ProfileOverviewComponent.State())
    override val state: Value<ProfileOverviewComponent.State> = _state

    private val scope = coroutineScope(Dispatchers.Main)

    init {
        // Reload on every resume (not just first creation) so edits made in Edit
        // profile — name, bio, photos — are reflected when this screen returns to
        // the foreground instead of showing stale init-time data.
        lifecycle.doOnResume(isOneTime = false) { load() }
    }

    private fun load() {
        scope.launch {
            val profile = runCatching { repositoryManager.getMyProfile() }.getOrNull()
            val did = runCatching { authRepository.getDid() }.getOrNull()
            val photos = profile?.photos
                ?.mapNotNull { ref -> runCatching { repositoryManager.blobFilePath(ref.ref) }.getOrNull() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
            _state.value = _state.value.copy(profile = profile, did = did, photos = photos)
        }
    }

    override fun onSettings() = onSettingsClick()
    override fun onEditProfile() = onEditProfileClick()
    override fun onRecoveryPhrase() = onRecoveryPhraseClick()
    override fun onBack() = onBackClick()

    override fun onSignOut() {
        if (_state.value.signingOut) return
        _state.value = _state.value.copy(signingOut = true)
        scope.launch {
            runCatching { peerProfileFeed.stop() }
            backgroundService.cancelBackgroundSync()
            backgroundService.setStayOnline(false)
            runCatching { authRepository.deleteAccount() }
            onSignedOut()
        }
    }
}
