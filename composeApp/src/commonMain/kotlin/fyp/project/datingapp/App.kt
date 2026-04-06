package fyp.project.datingapp

import androidx.compose.runtime.Composable
import fyp.project.datingapp.navigation.RootComponent
import fyp.project.datingapp.navigation.RootContent
import fyp.project.datingapp.ui.theme.TinderCloneComposeTheme

@Composable
fun App(component: RootComponent) {
    TinderCloneComposeTheme {
        RootContent(component)
    }
}