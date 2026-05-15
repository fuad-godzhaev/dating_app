package fyp.project.datingapp.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.StoreFactory
import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.database.appView.dao.IncomingLikesDao
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
import fyp.project.datingapp.feature.safety.BlockedUsersComponent
import fyp.project.datingapp.feature.safety.DefaultBlockedUsersComponent
import fyp.project.datingapp.feature.safety.DefaultReportComponent
import fyp.project.datingapp.feature.safety.DefaultReportSentComponent
import fyp.project.datingapp.feature.safety.ReportComponent
import fyp.project.datingapp.feature.safety.ReportSentComponent
import fyp.project.datingapp.feature.orbit.DefaultOrbitComponent
import fyp.project.datingapp.feature.orbit.OrbitComponent
import fyp.project.datingapp.feature.settings.DefaultChangePinComponent
import fyp.project.datingapp.feature.settings.ChangePinComponent
import fyp.project.datingapp.feature.settings.DefaultRecoveryPhraseComponent
import fyp.project.datingapp.feature.settings.RecoveryPhraseComponent
import fyp.project.datingapp.feature.legal.DefaultLegalComponent
import fyp.project.datingapp.feature.legal.LegalComponent
import fyp.project.datingapp.p2p.fetch.ProfileFetcher
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
        class Report(val component: ReportComponent) : Child()
        class ReportSent(val component: ReportSentComponent) : Child()
        class BlockedUsers(val component: BlockedUsersComponent) : Child()
        class Orbit(val component: OrbitComponent) : Child()
        class ChangePin(val component: ChangePinComponent) : Child()
        class RecoveryPhrase(val component: RecoveryPhraseComponent) : Child()
        class Legal(val component: LegalComponent) : Child()
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
    private val incomingLikesDao: IncomingLikesDao,
    private val profileFetcher: ProfileFetcher,
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
                    navigateToReport = { did, name -> navigation.push(Config.Report(did, name)) },
                )
            )
            Config.Messages -> RootComponent.Child.Messages(
                component = DefaultConversationListComponent(
                    componentContext = componentContext,
                    messageDao = messageDao,
                    incomingLikesDao = incomingLikesDao,
                    onOpen = { peerDid -> navigation.push(Config.Chat(peerDid)) },
                    onOrbitClick = { navigation.push(Config.Orbit) },
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
                    onReportClick = { did, name -> navigation.push(Config.Report(did, name)) },
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
                    onRecoveryPhraseClick = { navigation.push(Config.RecoveryPhrase) },
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
                    onChangePinClick = { navigation.push(Config.ChangePin) },
                    onBlockedUsersClick = { navigation.push(Config.BlockedUsers) },
                    onRecoveryPhraseClick = { navigation.push(Config.RecoveryPhrase) },
                    onPrivacyPolicyClick = { navigation.push(Config.Legal("PRIVACY")) },
                    onTermsOfUseClick = { navigation.push(Config.Legal("TERMS")) },
                    onSignedOut = { navigation.replaceAll(Config.SignUp) },
                    onBackClick = { navigation.pop() },
                )
            )
            is Config.Report -> RootComponent.Child.Report(
                component = DefaultReportComponent(
                    componentContext = componentContext,
                    targetName = config.targetName,
                    onBackClick = { navigation.pop() },
                    onSubmitted = { navigation.replaceCurrent(Config.ReportSent) },
                )
            )
            Config.ReportSent -> RootComponent.Child.ReportSent(
                component = DefaultReportSentComponent(
                    componentContext = componentContext,
                    onDoneClick = { navigation.pop() },
                )
            )
            Config.BlockedUsers -> RootComponent.Child.BlockedUsers(
                component = DefaultBlockedUsersComponent(
                    componentContext = componentContext,
                    onBackClick = { navigation.pop() },
                )
            )
            Config.Orbit -> RootComponent.Child.Orbit(
                component = DefaultOrbitComponent(
                    componentContext = componentContext,
                    incomingLikes = incomingLikesDao,
                    fetcher = profileFetcher,
                    likeService = likeService,
                    onBackClick = { navigation.pop() },
                )
            )
            Config.ChangePin -> RootComponent.Child.ChangePin(
                component = DefaultChangePinComponent(
                    componentContext = componentContext,
                    authRepository = authRepository,
                    onChanged = { navigation.pop() },
                    onBackClick = { navigation.pop() },
                )
            )
            Config.RecoveryPhrase -> RootComponent.Child.RecoveryPhrase(
                component = DefaultRecoveryPhraseComponent(
                    componentContext = componentContext,
                    onBackClick = { navigation.pop() },
                )
            )
            is Config.Legal -> RootComponent.Child.Legal(
                component = DefaultLegalComponent(
                    componentContext = componentContext,
                    kind = LegalComponent.Kind.valueOf(config.kind),
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
        @Serializable data class Report(val targetDid: String, val targetName: String) : Config
        @Serializable data object ReportSent : Config
        @Serializable data object BlockedUsers : Config
        @Serializable data object Orbit : Config
        @Serializable data object ChangePin : Config
        @Serializable data object RecoveryPhrase : Config
        @Serializable data class Legal(val kind: String) : Config
    }
}
