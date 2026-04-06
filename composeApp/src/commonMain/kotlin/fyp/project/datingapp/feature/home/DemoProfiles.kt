package fyp.project.datingapp.feature.home

import fyp.project.datingapp.records.UserProfile

/**
 * Hardcoded seed data for the Home swipe stack during Phase H (UI port).
 * Lets the visual + interaction layer be verified end-to-end without a
 * real peer feed.
 *
 * Replaced in Phase G.2 by a real `PeerProfileFeed` that reads from
 * `DiscoveryDao` / `RelayPolicy.get`. **One `git rm` removes this file.**
 *
 * Profiles are deliberately shallow:
 *  - `signingKey` + `signalPreKeyBundle` are empty — no peer verification
 *    runs against these.
 *  - `avatar` + `photos` are null so every card falls back to the
 *    deterministic placeholder colour for its `did`.
 */
internal object DemoProfiles {

    fun list(): List<UserProfile> = listOf(
        UserProfile(
            did = "did:key:zDemoAlice",
            displayName = "Alice",
            bio = "Climber, baker, and occasional philosopher.",
            age = 27,
            avatar = null,
            photos = null,
            signingKey = EMPTY,
            signalPreKeyBundle = EMPTY,
            interests = listOf("climbing", "baking", "books"),
            location = null,
            createdAt = "2026-04-23T00:00:00Z",
        ),
        UserProfile(
            did = "did:key:zDemoBen",
            displayName = "Ben",
            bio = "Runs a small coffee roastery, speaks three languages badly.",
            age = 31,
            avatar = null,
            photos = null,
            signingKey = EMPTY,
            signalPreKeyBundle = EMPTY,
            interests = listOf("coffee", "travel", "languages"),
            location = null,
            createdAt = "2026-04-23T00:00:00Z",
        ),
        UserProfile(
            did = "did:key:zDemoChloe",
            displayName = "Chloé",
            bio = "Cellist by day, bartender by night.",
            age = 24,
            avatar = null,
            photos = null,
            signingKey = EMPTY,
            signalPreKeyBundle = EMPTY,
            interests = listOf("music", "cocktails", "vinyl"),
            location = null,
            createdAt = "2026-04-23T00:00:00Z",
        ),
        UserProfile(
            did = "did:key:zDemoDan",
            displayName = "Dan",
            bio = "Weekend woodworker, weekday backend dev. Matches enjoy sawdust.",
            age = 29,
            avatar = null,
            photos = null,
            signingKey = EMPTY,
            signalPreKeyBundle = EMPTY,
            interests = listOf("woodworking", "backend", "hiking"),
            location = null,
            createdAt = "2026-04-23T00:00:00Z",
        ),
        UserProfile(
            did = "did:key:zDemoEva",
            displayName = "Eva",
            bio = "Sourdough obsessive. Will talk about crumb structure unprompted.",
            age = 26,
            avatar = null,
            photos = null,
            signingKey = EMPTY,
            signalPreKeyBundle = EMPTY,
            interests = listOf("baking", "farmers markets", "yoga"),
            location = null,
            createdAt = "2026-04-23T00:00:00Z",
        ),
    )

    private val EMPTY = ByteArray(0)
}
