package fyp.project.datingapp.feature.settings

import com.arkivanov.decompose.ComponentContext

/**
 * Recovery-phrase view (AURA_DESIGN_SPEC §6.12 -> recovery phrase view).
 *
 * The recovery phrase is derived one-way and is shown only once, at account creation; it is
 * not retained on the device (see interim-report notes). This screen therefore explains the
 * security posture rather than re-displaying the words.
 */
interface RecoveryPhraseComponent {
    fun onBack()
}

class DefaultRecoveryPhraseComponent(
    componentContext: ComponentContext,
    private val onBackClick: () -> Unit,
) : RecoveryPhraseComponent, ComponentContext by componentContext {
    override fun onBack() = onBackClick()
}
