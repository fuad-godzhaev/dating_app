package fyp.project.datingapp.feature.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import fyp.project.datingapp.domain.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Change PIN (AURA_DESIGN_SPEC §6.13). The two-step enter/confirm flow lives in the
 * content; this component just persists the confirmed PIN via [AuthRepository.setPin] and
 * returns to Settings on success.
 */
interface ChangePinComponent {
    val state: Value<State>
    fun submitNewPin(pin: String)
    fun onBack()

    data class State(val busy: Boolean = false, val error: String? = null)
}

class DefaultChangePinComponent(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val onChanged: () -> Unit,
    private val onBackClick: () -> Unit,
) : ChangePinComponent, ComponentContext by componentContext {

    private val _state = MutableValue(ChangePinComponent.State())
    override val state: Value<ChangePinComponent.State> = _state

    private val scope = coroutineScope(Dispatchers.Main)

    override fun submitNewPin(pin: String) {
        if (_state.value.busy) return
        _state.value = ChangePinComponent.State(busy = true)
        scope.launch {
            authRepository.setPin(pin)
                .onSuccess { onChanged() }
                .onFailure { _state.value = ChangePinComponent.State(error = "Could not change PIN. Try again.") }
        }
    }

    override fun onBack() = onBackClick()
}
