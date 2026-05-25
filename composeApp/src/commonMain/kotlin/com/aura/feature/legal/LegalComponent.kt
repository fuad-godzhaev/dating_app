package com.aura.feature.legal

import com.arkivanov.decompose.ComponentContext

/** Static legal text screen (Privacy policy / Terms of use), AURA_DESIGN_SPEC §6.12. */
interface LegalComponent {
    val kind: Kind
    fun onBack()

    enum class Kind { PRIVACY, TERMS }
}

class DefaultLegalComponent(
    componentContext: ComponentContext,
    override val kind: LegalComponent.Kind,
    private val onBackClick: () -> Unit,
) : LegalComponent, ComponentContext by componentContext {
    override fun onBack() = onBackClick()
}
