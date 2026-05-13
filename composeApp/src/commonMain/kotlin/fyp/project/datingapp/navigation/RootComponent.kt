package fyp.project.datingapp.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.StoreFactory
import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.database.appView.dao.MessageDao
import fyp.project.datingapp.domain.ResolveStartDestinationUseCase
import fyp.project.datingapp.domain.auth.AuthRepository
import fyp.project.datingapp.feature.chat.ChatComponent
import fyp.project.datingapp.feature.chat.ConversationListComponent
import fyp.project.datingapp.feature.chat.DefaultChatComponent
import fyp.project.datingapp.feature.chat.DefaultConversationListComponent
import fyp.project.datingapp.feature.home.DefaultHomeComponent
import fyp.project.datingapp.feature.home.HomeComponent
import fyp.project.datingapp.feature.profile.DefaultEditProfileComponent
import fyp.project.datingapp.feature.profile.DefaultProfileOverviewComponent
import fyp.project.datingapp.feature.profile.EditProfileComponent
import fyp.project.datingapp.feature.profile.ProfileOverviewComponent
import fyp.project.datingapp.feature.settings.DefaultSettingsComponent
import fyp.project.datingapp.feature.settings.SettingsComponent
import fyp.project.datingapp.p2p.messaging.MessageService
import fyp.project.datingapp.feature.onboarding.signin.DefaultSignInComponent
import fyp.project.datingapp.feature.onboarding.signin.SignInComponent
import fyp.project.datingapp.feature.onboarding.signup.DefaultSignUpComponent
import fyp.project.datingapp.feature.onboarding.signup.SignUpComponent
import fyp.project.datingapp.feature.splash.DefaultSplashComponent
import fyp.project.datingapp.feature.splash.SplashComponent
import fyp.project.datingapp.p2p.background.BackgroundService
import fyp.project.datingapp.p2p.blob.PhotoUploader
import fyp.project.datingapp.p2p.feed.PeerProfileFeed
import fyp.project.datingapp.p2p.like.LikeService
import fyp.project.datingapp.p2p.relay.RelayPolicy
import fyp.project.datingapp.p2p.relay.SessionInteractionTokens
import kotlinx.serialization.Serializable

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        class Splash(val component: SplashComponent) : Child()
        class SignIn(val component: SignInComponent) : Child()
        class SignUp(val component: SignUpComponent) : Child()
        class Home(val component: HomeComponent) : Child()
        class Messages(val component: ConversationListComponent) : Child()
        class Chat(val component: ChatComponent) : Child()
        class ProfileOverview(val component: ProfileOverviewComponent) : Child()
        class EditProfile(val component: EditProfileComponent) : Child()
        class Settings(val component: SettingsComponent) : Child()
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val repositoryManager: RepositoryManager,
    private val storeFactory: StoreFactory,
    private val peerProfileFeed: PeerProfileFeed,
    private val relayPolicy: RelayPolicy,
    private val sessionTokens: SessionInteractionTokens,
    private val messageService: MessageService,
    private val messageDao: MessageDao,
    private val likeService: LikeService,
    private val photoUploader: PhotoUploader,
    private val backgroundService: BackgroundService,
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
                    repositoryManager = repositoryManager,
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
                    relayPolicy = relayPolicy,
                    sessionTokens = sessionTokens,
                    likeService = likeService,
                    messageService = messageService,
                    navigateToEditProfile = { navigation.push(Config.ProfileOverview) },
                    navigateToMessages = { navigation.push(Config.Messages) },
                )
            )
            Config.Messages -> RootComponent.Child.Messages(
                component = DefaultConversationListComponent(
                    componentContext = componentContext,
                    messageDao = messageDao,
                    onOpen = { peerDid -> navigation.push(Config.Chat(peerDid)) },
                    onBackClick = { navigation.pop() },
                )
            )
            is Config.Chat -> RootComponent.Child.Chat(
                component = DefaultChatComponent(
                    componentContext = componentContext,
                    peerDid = config.peerDid,
                    messageDao = messageDao,
                    messageService = messageService,
                    onBackClick = { navigation.pop() },
                )
            )
            Config.ProfileOverview -> RootComponent.Child.ProfileOverview(
                component = DefaultProfileOverviewComponent(
                    componentContext = componentContext,
                    repositoryManager = repositoryManager,
                    authRepository = authRepository,
                    peerProfileFeed = peerProfileFeed,
                    backgroundService = backgroundService,
                    onSettingsClick = { navigation.push(Config.Settings) },
                    onEditProfileClick = { navigation.push(Config.EditProfile) },
                    onSignedOut = { navigation.replaceAll(Config.SignUp) },
                    onBackClick = { navigation.pop() },
                )
            )
            Config.EditProfile -> RootComponent.Child.EditProfile(
                component = DefaultEditProfileComponent(
                    componentContext = componentContext,
                    repositoryManager = repositoryManager,
                    photoUploader = photoUploader,
                    onSaved = { navigation.pop() },
                    onBackClick = { navigation.pop() },
                )
            )
            Config.Settings -> RootComponent.Child.Settings(
                component = DefaultSettingsComponent(
                    componentContext = componentContext,
                    authRepository = authRepository,
                    peerProfileFeed = peerProfileFeed,
                    backgroundService = backgroundService,
                    onChangePinClick = { /* TODO(Change PIN): dedicated screen */ },
                    onSignedOut = { navigation.replaceAll(Config.SignUp) },
                    onBackClick = { navigation.pop() },
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
        @Serializable data object Messages : Config
        @Serializable data class Chat(val peerDid: String) : Config
        @Serializable data object ProfileOverview : Config
        @Serializable data object EditProfile : Config
        @Serializable data object Settings : Config
    }
}
