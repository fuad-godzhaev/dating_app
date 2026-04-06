package fyp.project.datingapp.feature.splash

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import fyp.project.datingapp.domain.ResolveStartDestinationUseCase
import fyp.project.datingapp.domain.StartDestination
import kotlinx.coroutines.Dispatchers
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
            resolveStartDestination()
                .onSuccess { dest ->
                    when (dest) {
                        StartDestination.SignIn -> onNavigateToSignIn()
                        StartDestination.SignUp -> onNavigateToSignUp()
                    }
                }
                .onFailure { _state.value = SplashComponent.State.Error }
        }
    }
}
