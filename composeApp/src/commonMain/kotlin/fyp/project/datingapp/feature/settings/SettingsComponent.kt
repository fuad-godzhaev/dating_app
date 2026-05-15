package fyp.project.datingapp.feature.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import fyp.project.datingapp.domain.auth.AuthRepository
import fyp.project.datingapp.p2p.background.BackgroundService
import fyp.project.datingapp.p2p.feed.PeerProfileFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Settings screen (AURA_DESIGN_SPEC §6.12). */
interface SettingsComponent {
    val state: Value<State>
    fun onChangePin()
    fun onStayOnlineChanged(enabled: Boolean)
    fun onBlockedUsers()
    fun onRecoveryPhrase()
    fun onPrivacyPolicy()
    fun onTermsOfUse()
    fun onSignOut()
    fun onBack()

    data class State(
        val stayOnline: Boolean = false,
        val signingOut: Boolean = false,
    )
}

class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val peerProfileFeed: PeerProfileFeed,
    private val backgroundService: BackgroundService,
    private val onChangePinClick: () -> Unit,
    private val onBlockedUsersClick: () -> Unit = {},
    private val onRecoveryPhraseClick: () -> Unit = {},
    private val onPrivacyPolicyClick: () -> Unit = {},
    private val onTermsOfUseClick: () -> Unit = {},
    private val onSignedOut: () -> Unit,
    private val onBackClick: () -> Unit,
) : SettingsComponent, ComponentContext by componentContext {

    private val _state = MutableValue(SettingsComponent.State(stayOnline = backgroundService.isStayOnline()))
    override val state: Value<SettingsComponent.State> = _state

    private val scope = coroutineScope(Dispatchers.Main)

    override fun onChangePin() = onChangePinClick()
    override fun onBlockedUsers() = onBlockedUsersClick()
    override fun onRecoveryPhrase() = onRecoveryPhraseClick()
    override fun onPrivacyPolicy() = onPrivacyPolicyClick()
    override fun onTermsOfUse() = onTermsOfUseClick()
    override fun onBack() = onBackClick()

    /** Toggle the opt-in "Stay online" foreground service (Phase B). */
    override fun onStayOnlineChanged(enabled: Boolean) {
        backgroundService.setStayOnline(enabled)
        _state.value = _state.value.copy(stayOnline = enabled)
    }

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
