package com.apiguave.onboarding_ui.login

import androidx.activity.result.ActivityResult
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apiguave.auth_domain.model.Account
import com.apiguave.onboarding_ui.extensions.toProviderAccount
import com.apiguave.auth_domain.usecases.SignInUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val signInUseCase: SignInUseCase
) : ViewModel() {
    private val _eventChannel = Channel<LoginViewEvent>()
    val eventFlow = _eventChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow<LoginViewState>(LoginViewState.Ready)
    val uiState = _uiState.asStateFlow()

    /**
     * Sign in with email and password.
     */
    fun signIn(email: String, password: String) = viewModelScope.launch {
        if (email.isBlank() || password.isBlank()) {
            _eventChannel.send(LoginViewEvent.LoginError("Please enter both email and password"))
            return@launch
        }

        _uiState.update { LoginViewState.Loading }
        val account = Account(email = email, password = password)

        signInUseCase(account).onSuccess {
            _eventChannel.send(LoginViewEvent.NavigateHome)
        }.onFailure { error ->
            _eventChannel.send(LoginViewEvent.LoginError(error.message ?: "Sign in failed"))
        }

        _uiState.update { LoginViewState.Ready }
    }

    fun navigateToRegister() = viewModelScope.launch {
        _eventChannel.send(LoginViewEvent.NavigateToRegister)
    }

    // Legacy Google Sign-In - COMMENTED OUT
    /*
    fun signIn(activityResult: ActivityResult) = viewModelScope.launch {
        val account: Account = try {
            activityResult.toProviderAccount()
        } catch (e: Exception) {
            _eventChannel.send(LoginViewEvent.LoginError)
            return@launch
        }

        _uiState.update { LoginViewState.Loading }
        signInUseCase(account).fold({
            _eventChannel.send(LoginViewEvent.NavigateHome)
        }, { _ ->
            _eventChannel.send(LoginViewEvent.LoginError)
        })

    }
    */
}

@Immutable
sealed class LoginViewEvent {
    data object NavigateHome : LoginViewEvent()
    data object NavigateToRegister : LoginViewEvent()
    data class LoginError(val message: String) : LoginViewEvent()
}

@Immutable
sealed class LoginViewState {
    data object Loading : LoginViewState()
    data object Ready : LoginViewState()
}