package fyp.project.datingapp.feature.safety

import com.arkivanov.decompose.ComponentContext

/**
 * Blocked users list (AURA_DESIGN_SPEC §6.12 PRIVACY & SAFETY).
 *
 * UI-only: there is no persisted block list yet (see interim-report notes), so this screen
 * currently shows an empty state.
 */
interface BlockedUsersComponent {
    fun onBack()
}

class DefaultBlockedUsersComponent(
    componentContext: ComponentContext,
    private val onBackClick: () -> Unit,
) : BlockedUsersComponent, ComponentContext by componentContext {
    override fun onBack() = onBackClick()
}
