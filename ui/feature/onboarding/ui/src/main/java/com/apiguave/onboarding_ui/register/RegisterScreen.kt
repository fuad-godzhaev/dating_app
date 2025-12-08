package com.apiguave.onboarding_ui.register

import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToCreateProfile: () -> Unit
) {
    val viewModel: RegisterViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.eventFlow.collect { event ->
                when (event) {
                    is RegisterViewEvent.RegisterError -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(event.message)
                        }
                    }
                    RegisterViewEvent.NavigateToCreateProfile -> onNavigateToCreateProfile()
                    RegisterViewEvent.NavigateToLogin -> onNavigateToLogin()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { _ ->
        RegisterView(
            uiState = uiState,
            onRegisterClicked = { email, password, confirmPassword ->
                viewModel.register(email, password, confirmPassword)
            },
            onLoginClicked = {
                viewModel.navigateToLogin()
            }
        )
    }
}
