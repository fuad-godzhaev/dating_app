package com.aura.feature.safety

import com.arkivanov.decompose.ComponentContext

/**
 * Report a user (AURA_DESIGN_SPEC §6.16).
 *
 * UI-only: this app is serverless, so a report has no server to reach and the other user is
 * never notified. Submitting records nothing and simply advances to the confirmation screen.
 * Real report/block persistence + discovery filtering is out of scope (documented as future
 * work in the interim report).
 */
interface ReportComponent {
    val targetName: String
    fun onBack()
    fun onSubmit()
}

class DefaultReportComponent(
    componentContext: ComponentContext,
    override val targetName: String,
    private val onBackClick: () -> Unit,
    private val onSubmitted: () -> Unit,
) : ReportComponent, ComponentContext by componentContext {
    override fun onBack() = onBackClick()
    override fun onSubmit() = onSubmitted()
}
