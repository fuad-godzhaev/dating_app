package fyp.project.datingapp.p2p.discovery

import fyp.project.datingapp.p2p.transport.wire.AgeRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Matchmaking attributes a [fyp.project.datingapp.p2p.transport.wire.PresenceRecord]
 * advertises that the current [fyp.project.datingapp.records.UserProfile] does
 * not carry (`gender`, `lookingFor`, `ageRange`). `interests` come from the
 * profile; only these three live here.
 *
 * Phase C keeps this **local and transient** (in-memory, sensible defaults) —
 * discovery is verified via the debug harness, not the onboarding UI.
 * TODO(Phase G/onboarding): collect these in onboarding and persist them
 * (Room or settings) so they survive a restart; for now they reset to defaults.
 */
data class DiscoveryPreferences(
    val gender: String = GENDER_UNSPECIFIED,
    val lookingFor: List<String> = listOf(LOOKING_FOR_EVERYONE),
    val ageRange: AgeRange = AgeRange(min = 18, max = 99),
) {
    companion object {
        const val GENDER_UNSPECIFIED = "unspecified"
        const val LOOKING_FOR_EVERYONE = "everyone"
    }
}

/**
 * In-memory holder for the local user's [DiscoveryPreferences]. A single Koin
 * singleton shared by the announcer (reads on each heartbeat) and any future
 * settings UI / the debug harness (writes via [update]).
 */
class DiscoveryPreferencesStore {
    private val _prefs = MutableStateFlow(DiscoveryPreferences())
    val prefs: StateFlow<DiscoveryPreferences> = _prefs.asStateFlow()

    fun current(): DiscoveryPreferences = _prefs.value
    fun update(prefs: DiscoveryPreferences) { _prefs.value = prefs }
}
