package fyp.project.datingapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import fyp.project.datingapp.feature.chat.ChatContent
import fyp.project.datingapp.feature.chat.ConversationListContent
import fyp.project.datingapp.feature.home.HomeContent
import fyp.project.datingapp.feature.onboarding.signin.SignInContent
import fyp.project.datingapp.feature.onboarding.signup.SignUpContent
import fyp.project.datingapp.feature.splash.SplashContent

@Composable
fun RootContent(component: RootComponent) {
    val stack by component.stack.subscribeAsState()
    Children(stack = stack) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.Splash -> SplashContent(instance.component)
            is RootComponent.Child.SignIn -> SignInContent(
                state = instance.component.state.subscribeAsState().value,
                onDigitEntered = instance.component::onDigitEntered,
                onBackspace = instance.component::onBackspace,
                onForgotPin = instance.component::onForgotPin,
            )
            is RootComponent.Child.SignUp -> {
                val state by instance.component.state.subscribeAsState()
                SignUpContent(
                    state = state,
                    onCreateNewAccount = instance.component::onCreateNewAccount,
                    onRestoreExistingAccount = instance.component::onRestoreExistingAccount,
                    onSeedPhraseWrittenDown = instance.component::onSeedPhraseWrittenDown,
                    onSeedWordChanged = instance.component::onSeedWordChanged,
                    onConfirmSeedPhrase = instance.component::onConfirmSeedPhrase,
                    onPinDigitEntered = instance.component::onPinDigitEntered,
                    onPinBackspace = instance.component::onPinBackspace,
                    onDisplayNameChanged = instance.component::onDisplayNameChanged,
                    onBioChanged = instance.component::onBioChanged,
                    onAgeChanged = instance.component::onAgeChanged,
                    onInterestsChanged = instance.component::onInterestsChanged,
                    onCreateProfile = instance.component::onCreateProfile,
                    onBack = instance.component::onBack,
                )
            }
            is RootComponent.Child.Home -> HomeContent(instance.component)
            is RootComponent.Child.Messages -> ConversationListContent(instance.component)
            is RootComponent.Child.Chat -> ChatContent(instance.component)
        }
    }
}
