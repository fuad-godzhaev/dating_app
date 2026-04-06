package fyp.project.datingapp.feature.onboarding.signin

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import fyp.project.datingapp.domain.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface SignInComponent {
    val state: Value<State>

    fun onDigitEntered(digit: Char)
    fun onBackspace()
    fun onForgotPin()

    data class State(
        val enteredDigits: String = "",
        val isVerifying: Boolean = false,
        val error: String? = null,
        val attemptsRemaining: Int = MAX_ATTEMPTS,
        val isLockedOut: Boolean = false
    ) {
        val digitCount: Int get() = enteredDigits.length
        val isComplete: Boolean get() = enteredDigits.length == PIN_LENGTH
    }

    companion object {
        const val PIN_LENGTH = 4
        const val MAX_ATTEMPTS = 5
    }
}

class DefaultSignInComponent(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val onNavigateToHome: () -> Unit,
    private val onNavigateToRestore: () -> Unit,
) : SignInComponent, ComponentContext by componentContext {

    private val _state = MutableValue(SignInComponent.State())
    override val state: Value<SignInComponent.State> = _state

    private val scope = coroutineScope(Dispatchers.Main)

    override fun onDigitEntered(digit: Char) {
        val current = _state.value
        if (current.isLockedOut || current.isVerifying) return
        if (current.enteredDigits.length >= SignInComponent.PIN_LENGTH) return

        val updated = current.enteredDigits + digit
        _state.value = current.copy(enteredDigits = updated, error = null)

        if (updated.length == SignInComponent.PIN_LENGTH) {
            verifyPin(updated)
        }
    }

    override fun onBackspace() {
        val current = _state.value
        if (current.isVerifying || current.enteredDigits.isEmpty()) return
        _state.value = current.copy(enteredDigits = current.enteredDigits.dropLast(1), error = null)
    }

    override fun onForgotPin() {
        onNavigateToRestore()
    }

    private fun verifyPin(pin: String) {
        _state.value = _state.value.copy(isVerifying = true)
        scope.launch {
            val isValid = authRepository.verifyPin(pin)
            if (isValid) {
                _state.value = _state.value.copy(isVerifying = false)
                onNavigateToHome()
            } else {
                val remaining = _state.value.attemptsRemaining - 1
                _state.value = _state.value.copy(
                    enteredDigits = "",
                    isVerifying = false,
                    error = if (remaining > 0) "Incorrect PIN. $remaining attempts remaining."
                            else "Too many attempts. Use seed phrase to restore.",
                    attemptsRemaining = remaining,
                    isLockedOut = remaining <= 0
                )
            }
        }
    }
}
