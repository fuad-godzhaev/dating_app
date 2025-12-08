package com.apiguave.onboarding_ui.register

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apiguave.auth_domain.model.Account
import com.apiguave.auth_domain.usecases.SignUpUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {
    private val _eventChannel = Channel<RegisterViewEvent>()
    val eventFlow = _eventChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow<RegisterViewState>(RegisterViewState.Ready)
    val uiState = _uiState.asStateFlow()

    /**
     * Register with email and password.
     */
    fun register(email: String, password: String, confirmPassword: String) = viewModelScope.launch {
        if (email.isBlank() || password.isBlank()) {
            _eventChannel.send(RegisterViewEvent.RegisterError("Email and password cannot be empty"))
            return@launch
        }

        if (password != confirmPassword) {
            _eventChannel.send(RegisterViewEvent.RegisterError("Passwords do not match"))
            return@launch
        }

        if (password.length < 6) {
            _eventChannel.send(RegisterViewEvent.RegisterError("Password must be at least 6 characters"))
            return@launch
        }

        _uiState.update { RegisterViewState.Loading }

        val account = Account(email = email, password = password)

        signUpUseCase(account).onSuccess {
            _eventChannel.send(RegisterViewEvent.NavigateToCreateProfile)
        }.onFailure { error ->
            _eventChannel.send(RegisterViewEvent.RegisterError(error.message ?: "Registration failed"))
        }

        _uiState.update { RegisterViewState.Ready }
    }

    /**
     * Navigate back to login screen.
     */
    fun navigateToLogin() = viewModelScope.launch {
        _eventChannel.send(RegisterViewEvent.NavigateToLogin)
    }
}

@Immutable
sealed class RegisterViewEvent {
    data object NavigateToCreateProfile : RegisterViewEvent()
    data object NavigateToLogin : RegisterViewEvent()
    data class RegisterError(val message: String) : RegisterViewEvent()
}

@Immutable
sealed class RegisterViewState {
    data object Loading : RegisterViewState()
    data object Ready : RegisterViewState()
}
