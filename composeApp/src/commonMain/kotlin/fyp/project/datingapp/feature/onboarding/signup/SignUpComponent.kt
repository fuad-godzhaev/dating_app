package fyp.project.datingapp.feature.onboarding.signup

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.domain.auth.AuthRepository
import fyp.project.datingapp.domain.auth.SeedPhrase
import fyp.project.datingapp.records.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Clock

interface SignUpComponent {
    val state: Value<State>

    fun onCreateNewAccount()
    fun onRestoreExistingAccount()
    fun onSeedPhraseWrittenDown()
    fun onSeedWordChanged(index: Int, word: String)
    fun onConfirmSeedPhrase()
    fun onPinDigitEntered(digit: Char)
    fun onPinBackspace()
    fun onDisplayNameChanged(value: String)
    fun onBioChanged(value: String)
    fun onAgeChanged(value: Int)
    fun onInterestsChanged(value: List<String>)
    fun onCreateProfile()
    fun onBack()

    data class State(
        val step: Step = Step.Welcome,
        val isLoading: Boolean = false,
        val error: String? = null,
        val generatedSeedPhrase: SeedPhrase? = null,
        val enteredSeedWords: List<String> = List(12) { "" },
        val pin: String = "",
        val pinConfirm: String = "",
        val isConfirmingPin: Boolean = false,
        val displayName: String = "",
        val bio: String = "",
        val age: Int = 18,
        val interests: List<String> = emptyList(),
    ) {
        val pinDigitCount: Int get() = if (isConfirmingPin) pinConfirm.length else pin.length
        val canCreateProfile: Boolean get() = displayName.isNotBlank() && age >= 18
    }

    enum class Step {
        Welcome,
        GeneratingIdentity,
        ShowSeedPhrase,
        EnterSeedPhrase,
        SetPin,
        CreateProfile,
    }
}

class DefaultSignUpComponent(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val repositoryManager: RepositoryManager,
    private val onNavigateToHome: () -> Unit,
) : SignUpComponent, ComponentContext by componentContext {

    private val _state = MutableValue(SignUpComponent.State())
    override val state: Value<SignUpComponent.State> = _state

    private val scope = coroutineScope(Dispatchers.Main)

    override fun onCreateNewAccount() {
        _state.value = _state.value.copy(
            step = SignUpComponent.Step.GeneratingIdentity,
            isLoading = true,
            error = null
        )
        scope.launch {
            authRepository.generateIdentity()
                .onSuccess { seedPhrase ->
                    _state.value = _state.value.copy(
                        step = SignUpComponent.Step.ShowSeedPhrase,
                        isLoading = false,
                        generatedSeedPhrase = seedPhrase
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        step = SignUpComponent.Step.Welcome,
                        isLoading = false,
                        error = e.message ?: "Failed to generate identity"
                    )
                }
        }
    }

    override fun onRestoreExistingAccount() {
        _state.value = _state.value.copy(step = SignUpComponent.Step.EnterSeedPhrase, error = null)
    }

    override fun onSeedPhraseWrittenDown() {
        _state.value = _state.value.copy(step = SignUpComponent.Step.SetPin, error = null)
    }

    override fun onSeedWordChanged(index: Int, word: String) {
        val words = _state.value.enteredSeedWords.toMutableList()
        words[index] = word.lowercase().trim()
        _state.value = _state.value.copy(enteredSeedWords = words, error = null)
    }

    override fun onConfirmSeedPhrase() {
        val words = _state.value.enteredSeedWords
        if (words.any { it.isBlank() }) {
            _state.value = _state.value.copy(error = "Please fill in all 12 words")
            return
        }
        _state.value = _state.value.copy(isLoading = true, error = null)
        scope.launch {
            try {
                val seedPhrase = SeedPhrase(words)
                authRepository.restoreIdentity(seedPhrase)
                    .onSuccess {
                        _state.value = _state.value.copy(step = SignUpComponent.Step.SetPin, isLoading = false)
                    }
                    .onFailure { e ->
                        _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to restore identity")
                    }
            } catch (e: IllegalArgumentException) {
                _state.value = _state.value.copy(isLoading = false, error = "Seed phrase must be exactly 12 words")
            }
        }
    }

    override fun onPinDigitEntered(digit: Char) {
        val current = _state.value
        if (current.isLoading) return
        if (!current.isConfirmingPin) {
            val updated = current.pin + digit
            _state.value = current.copy(pin = updated, error = null)
            if (updated.length == 4) {
                _state.value = _state.value.copy(isConfirmingPin = true)
            }
        } else {
            val updated = current.pinConfirm + digit
            _state.value = current.copy(pinConfirm = updated, error = null)
            if (updated.length == 4) {
                if (current.pin == updated) {
                    savePin(current.pin)
                } else {
                    _state.value = _state.value.copy(
                        pinConfirm = "",
                        isConfirmingPin = false,
                        pin = "",
                        error = "PINs don't match. Try again."
                    )
                }
            }
        }
    }

    override fun onPinBackspace() {
        val current = _state.value
        if (current.isConfirmingPin && current.pinConfirm.isNotEmpty()) {
            _state.value = current.copy(pinConfirm = current.pinConfirm.dropLast(1))
        } else if (!current.isConfirmingPin && current.pin.isNotEmpty()) {
            _state.value = current.copy(pin = current.pin.dropLast(1))
        }
    }

    private fun savePin(pin: String) {
        _state.value = _state.value.copy(isLoading = true)
        scope.launch {
            authRepository.setPin(pin)
                .onSuccess {
                    _state.value = _state.value.copy(step = SignUpComponent.Step.CreateProfile, isLoading = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false, pin = "", pinConfirm = "", isConfirmingPin = false,
                        error = e.message ?: "Failed to save PIN"
                    )
                }
        }
    }

    override fun onDisplayNameChanged(value: String) {
        _state.value = _state.value.copy(displayName = value)
    }

    override fun onBioChanged(value: String) {
        _state.value = _state.value.copy(bio = value)
    }

    override fun onAgeChanged(value: Int) {
        _state.value = _state.value.copy(age = value)
    }

    override fun onInterestsChanged(value: List<String>) {
        _state.value = _state.value.copy(interests = value)
    }

    override fun onCreateProfile() {
        val current = _state.value
        if (!current.canCreateProfile) return
        _state.value = current.copy(isLoading = true, error = null)
        scope.launch {
            val identity = authRepository.getIdentity()
            if (identity == null) {
                _state.value = current.copy(isLoading = false, error = "Identity not found — this shouldn't happen")
                return@launch
            }
            val profile = UserProfile(
                did = identity.did,
                displayName = current.displayName,
                bio = current.bio,
                age = current.age,
                interests = current.interests,
                signingKey = identity.publicKey,
                createdAt = Clock.System.now().toString()
            )
            repositoryManager.putProfile(profile)
                .onSuccess {
                    _state.value = current.copy(isLoading = false)
                    onNavigateToHome()
                }
                .onFailure { e ->
                    _state.value = current.copy(isLoading = false, error = e.message ?: "Failed to create profile")
                }
        }
    }

    override fun onBack() {
        val current = _state.value
        _state.value = when (current.step) {
            SignUpComponent.Step.Welcome -> return
            SignUpComponent.Step.GeneratingIdentity -> current.copy(step = SignUpComponent.Step.Welcome)
            SignUpComponent.Step.ShowSeedPhrase -> current.copy(step = SignUpComponent.Step.Welcome)
            SignUpComponent.Step.EnterSeedPhrase -> current.copy(step = SignUpComponent.Step.Welcome)
            SignUpComponent.Step.SetPin -> current.copy(
                step = SignUpComponent.Step.Welcome, pin = "", pinConfirm = "", isConfirmingPin = false
            )
            SignUpComponent.Step.CreateProfile -> current.copy(step = SignUpComponent.Step.SetPin)
        }
    }
}
