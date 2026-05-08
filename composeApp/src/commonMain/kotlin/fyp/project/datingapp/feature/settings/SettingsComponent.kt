package fyp.project.datingapp.feature.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import fyp.project.datingapp.domain.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Settings screen (AURA_DESIGN_SPEC §6.12). */
interface SettingsComponent {
    val state: Value<State>
    fun onChangePin()
    fun onSignOut()
    fun onBack()

    data class State(val signingOut: Boolean = false)
}

class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val onChangePinClick: () -> Unit,
    private val onSignedOut: () -> Unit,
    private val onBackClick: () -> Unit,
) : SettingsComponent, ComponentContext by componentContext {

    private val _state = MutableValue(SettingsComponent.State())
    override val state: Value<SettingsComponent.State> = _state

    private val scope = coroutineScope(Dispatchers.Main)

    override fun onChangePin() = onChangePinClick()
    override fun onBack() = onBackClick()

    override fun onSignOut() {
        if (_state.value.signingOut) return
        _state.value = _state.value.copy(signingOut = true)
        scope.launch {
            // TODO(B): also peerProfileFeed.stop() + cancel FGS/WorkManager before deleting.
            runCatching { authRepository.deleteAccount() }
            onSignedOut()
        }
    }
}
