package fyp.project.datingapp.feature.safety

import com.arkivanov.decompose.ComponentContext

/** Report confirmation (AURA_DESIGN_SPEC §6.17). "Done" returns to the originating screen. */
interface ReportSentComponent {
    fun onDone()
}

class DefaultReportSentComponent(
    componentContext: ComponentContext,
    private val onDoneClick: () -> Unit,
) : ReportSentComponent, ComponentContext by componentContext {
    override fun onDone() = onDoneClick()
}
