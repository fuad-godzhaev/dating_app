package fyp.project.datingapp.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Locks in the shape of the Phase H demo seed. If/when Phase G.2 wires
 * a real peer feed and deletes [DemoProfiles], these tests go with it
 * — but until then they keep the swipe stack from silently going empty
 * (e.g. someone collapses the list to 1 entry while debugging and the
 * "no_more_profiles" path renders on first launch).
 */
class DemoProfilesTest {

    @Test
    fun yieldsBetween3And5DistinctDemoProfiles() {
        val profiles = DemoProfiles.list()
        assertTrue(profiles.size in 3..5, "expected 3..5 demo profiles, got ${profiles.size}")
        val dids = profiles.map { it.did }
        assertEquals(dids.distinct().size, dids.size, "demo dids must be unique")
    }

    @Test
    fun everyProfileHasARenderableMinimum() {
        DemoProfiles.list().forEach { profile ->
            assertNotNull(profile.displayName.takeIf { it.isNotBlank() })
            assertTrue(profile.age in 18..120, "age out of human range: ${profile.age}")
            assertTrue(profile.bio.isNotBlank(), "bio must be non-blank for ${profile.did}")
            // Pictures intentionally null — the placeholder backdrop path is
            // what we want exercised in Phase H, not Coil/blob fetch.
            assertNull(profile.avatar)
            assertNull(profile.photos)
        }
    }
}
