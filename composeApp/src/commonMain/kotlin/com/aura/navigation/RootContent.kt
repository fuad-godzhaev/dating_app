package com.aura.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.aura.feature.chat.ChatContent
import com.aura.feature.chat.ConversationListContent
import com.aura.feature.home.HomeContent
import com.aura.feature.onboarding.signin.SignInContent
import com.aura.feature.onboarding.signup.SignUpContent
import com.aura.feature.profile.EditProfileContent
import com.aura.feature.profile.ProfileOverviewContent
import com.aura.feature.safety.BlockedUsersContent
import com.aura.feature.safety.ReportContent
import com.aura.feature.safety.ReportSentContent
import com.aura.feature.orbit.OrbitContent
import com.aura.feature.legal.LegalContent
import com.aura.feature.settings.ChangePinContent
import com.aura.feature.settings.RecoveryPhraseContent
import com.aura.feature.settings.SettingsContent
import com.aura.feature.splash.SplashContent

@Composable
fun RootContent(component: RootComponent) {
    val stack by component.stack.subscribeAsState()
    // Per-transition motion (spec §7): dissolve for splash hand-off and the report
    // confirmation, a fade+scale "enter the app" for onboarding -> Home, and the
    // default iOS-style horizontal slide for ordinary push/pop.
    Children(
        stack = stack,
        animation = stackAnimation { child, otherChild, _ ->
            val a = child.instance
            val b = otherChild.instance
            when {
                a is RootComponent.Child.Splash || b is RootComponent.Child.Splash -> fade()
                a is RootComponent.Child.ReportSent || b is RootComponent.Child.ReportSent -> fade()
                isEnterApp(a, b) -> fade() + scale()
                else -> slide()
            }
        },
    ) { child ->
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
            is RootComponent.Child.ProfileOverview -> ProfileOverviewContent(instance.component)
            is RootComponent.Child.EditProfile -> EditProfileContent(instance.component)
            is RootComponent.Child.Settings -> SettingsContent(instance.component)
            is RootComponent.Child.Report -> ReportContent(instance.component)
            is RootComponent.Child.ReportSent -> ReportSentContent(instance.component)
            is RootComponent.Child.BlockedUsers -> BlockedUsersContent(instance.component)
            is RootComponent.Child.Orbit -> OrbitContent(instance.component)
            is RootComponent.Child.ChangePin -> ChangePinContent(instance.component)
            is RootComponent.Child.RecoveryPhrase -> RecoveryPhraseContent(instance.component)
            is RootComponent.Child.Legal -> LegalContent(instance.component)
        }
    }
}

/** True for the onboarding -> Home hand-off, which "moves into" the app. */
private fun isEnterApp(a: RootComponent.Child, b: RootComponent.Child): Boolean {
    val onboarding = setOf(RootComponent.Child.SignIn::class, RootComponent.Child.SignUp::class)
    return (a is RootComponent.Child.Home && b::class in onboarding) ||
        (b is RootComponent.Child.Home && a::class in onboarding)
}
