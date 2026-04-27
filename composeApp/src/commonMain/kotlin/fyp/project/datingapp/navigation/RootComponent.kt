package fyp.project.datingapp.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.StoreFactory
import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.domain.ResolveStartDestinationUseCase
import fyp.project.datingapp.domain.auth.AuthRepository
import fyp.project.datingapp.feature.home.DefaultHomeComponent
import fyp.project.datingapp.feature.home.HomeComponent
import fyp.project.datingapp.feature.onboarding.signin.DefaultSignInComponent
import fyp.project.datingapp.feature.onboarding.signin.SignInComponent
import fyp.project.datingapp.feature.onboarding.signup.DefaultSignUpComponent
import fyp.project.datingapp.feature.onboarding.signup.SignUpComponent
import fyp.project.datingapp.feature.splash.DefaultSplashComponent
import fyp.project.datingapp.feature.splash.SplashComponent
import fyp.project.datingapp.p2p.feed.PeerProfileFeed
import kotlinx.serialization.Serializable

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        class Splash(val component: SplashComponent) : Child()
        class SignIn(val component: SignInComponent) : Child()
        class SignUp(val component: SignUpComponent) : Child()
        class Home(val component: HomeComponent) : Child()
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val repositoryManager: RepositoryManager,
    private val storeFactory: StoreFactory,
    private val peerProfileFeed: PeerProfileFeed,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Splash,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(config: Config, componentContext: ComponentContext): RootComponent.Child {
        return when (config) {
            Config.Splash -> RootComponent.Child.Splash(
                component = DefaultSplashComponent(
                    componentContext = componentContext,
                    resolveStartDestination = ResolveStartDestinationUseCase(
                        auth = authRepository,
                        repositoryManager = repositoryManager,
                    ),
                    onNavigateToSignIn = { navigation.replaceAll(Config.SignIn) },
                    onNavigateToSignUp = { navigation.replaceAll(Config.SignUp) },
                )
            )
            Config.SignIn -> RootComponent.Child.SignIn(
                component = DefaultSignInComponent(
                    componentContext = componentContext,
                    authRepository = authRepository,
                    onNavigateToHome = { navigation.replaceAll(Config.Home) },
                    onNavigateToRestore = { navigation.replaceAll(Config.SignUp) },
                )
            )
            Config.SignUp -> RootComponent.Child.SignUp(
                component = DefaultSignUpComponent(
                    componentContext = componentContext,
                    authRepository = authRepository,
                    repositoryManager = repositoryManager,
                    onNavigateToHome = { navigation.replaceAll(Config.Home) },
                )
            )
            Config.Home -> RootComponent.Child.Home(
                component = DefaultHomeComponent(
                    componentContext = componentContext,
                    storeFactory = storeFactory,
                    repositoryManager = repositoryManager,
                    peerProfileFeed = peerProfileFeed,
                )
            )
        }
    }

    @Serializable
    private sealed interface Config {
        @Serializable data object Splash : Config
        @Serializable data object SignIn : Config
        @Serializable data object SignUp : Config
        @Serializable data object Home : Config
    }
}
