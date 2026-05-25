package com.aura.feature.splash

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.aura.domain.ResolveStartDestinationUseCase
import com.aura.domain.StartDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface SplashComponent {
    val state: Value<State>
    fun onRetry()

    sealed interface State {
        data object Loading : State
        data object Error : State
    }
}

class DefaultSplashComponent(
    componentContext: ComponentContext,
    private val resolveStartDestination: ResolveStartDestinationUseCase,
    private val onNavigateToSignIn: () -> Unit,
    private val onNavigateToSignUp: () -> Unit,
) : SplashComponent, ComponentContext by componentContext {

    private val _state = MutableValue<SplashComponent.State>(SplashComponent.State.Loading)
    override val state: Value<SplashComponent.State> = _state

    private val scope = coroutineScope(Dispatchers.Main)

    init { resolve() }

    override fun onRetry() = resolve()

    private fun resolve() {
        _state.value = SplashComponent.State.Loading
        scope.launch {
            // Keep the branded splash on screen for a minimum dwell (spec §6.8) so the
            // logo/loader is seen even when the start destination resolves instantly,
            // then hand off to Unlock/onboarding.
            val dwell = launch { delay(MIN_DWELL_MS) }
            resolveStartDestination()
                .onSuccess { dest ->
                    dwell.join()
                    when (dest) {
                        StartDestination.SignIn -> onNavigateToSignIn()
                        StartDestination.SignUp -> onNavigateToSignUp()
                    }
                }
                .onFailure { _state.value = SplashComponent.State.Error }
        }
    }

    companion object {
        private const val MIN_DWELL_MS = 1400L
    }
}
