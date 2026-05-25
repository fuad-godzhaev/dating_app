package com.aura.p2p.discovery

import com.aura.p2p.transport.wire.AgeRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Matchmaking attributes a [com.aura.p2p.transport.wire.PresenceRecord]
 * advertises that the current [com.aura.records.UserProfile] does
 * not carry (`gender`, `lookingFor`, `ageRange`). `interests` come from the
 * profile; only these three live here.
 *
 * `ageRange` is now advertised from the user's real profile age by [PresenceAnnouncer] (so peers'
 * age filters match real values). `gender`/`lookingFor` still live here, in-memory with sensible
 * defaults, because there is no production writer yet (Settings toggles are UI-only; the announcer
 * only reads). Until a Settings/onboarding screen collects + persists them, every peer advertises
 * the default gender ("unspecified") - so a peer's GENDER feed-filter only matches users who have
 * set one.
 * TODO(onboarding/settings): collect `gender`/`lookingFor` in the UI and persist them (mirror
 * `FeedFilterStore`) so the gender filter becomes meaningful; until then keep the gender chip hidden
 * in the Home filter sheet or treat it as advisory.
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
