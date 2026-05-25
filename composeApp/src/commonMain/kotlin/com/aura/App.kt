package com.aura

import androidx.compose.runtime.Composable
import com.aura.navigation.RootComponent
import com.aura.navigation.RootContent
import com.aura.ui.theme.aura.AuraTheme

@Composable
fun App(component: RootComponent) {
    AuraTheme {
        RootContent(component)
    }
}
