package fyp.project.datingapp.feature.onboarding.signin

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import fyp.project.datingapp.database.RepositoryManager
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
        val incorrectAttempts: Int = 0,
        val suggestRecovery: Boolean = false,
        val displayName: String? = null,
    ) {
        val digitCount: Int get() = enteredDigits.length
        val isComplete: Boolean get() = enteredDigits.length == PIN_LENGTH
    }

    companion object {
        const val PIN_LENGTH = 4
        // PIN attempts are unlimited (no lockout). After this many wrong tries we
        // proactively suggest restoring the account from the recovery phrase.
        const val SUGGEST_RECOVERY_AFTER = 3
    }
}

class DefaultSignInComponent(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val repositoryManager: RepositoryManager,
    private val onNavigateToHome: () -> Unit,
    private val onNavigateToRestore: () -> Unit,
) : SignInComponent, ComponentContext by componentContext {

    private val _state = MutableValue(SignInComponent.State())
    override val state: Value<SignInComponent.State> = _state

    private val scope = coroutineScope(Dispatchers.Main)

    init {
        // Load the display name for the "Welcome back, {name}" greeting (best-effort).
        scope.launch {
            val name = runCatching { repositoryManager.getMyProfile()?.displayName }.getOrNull()
            if (name != null) _state.value = _state.value.copy(displayName = name)
        }
    }

    override fun onDigitEntered(digit: Char) {
        val current = _state.value
        if (current.isVerifying) return
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
                val attempts = _state.value.incorrectAttempts + 1
                val suggest = attempts >= SignInComponent.SUGGEST_RECOVERY_AFTER
                _state.value = _state.value.copy(
                    enteredDigits = "",
                    isVerifying = false,
                    incorrectAttempts = attempts,
                    suggestRecovery = suggest,
                    error = if (suggest)
                        "Incorrect PIN. Forgot it? You can restore your account with your recovery phrase."
                    else
                        "Incorrect PIN. Try again.",
                )
            }
        }
    }
}
