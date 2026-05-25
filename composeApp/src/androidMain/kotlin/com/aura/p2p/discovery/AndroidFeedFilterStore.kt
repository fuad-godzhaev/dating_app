package com.aura.p2p.discovery

import android.content.Context

/** Android [FeedFilterStore] backed by SharedPreferences. */
class AndroidFeedFilterStore(context: Context) : FeedFilterStore {
    private val prefs = context.getSharedPreferences("aura.feedfilters", Context.MODE_PRIVATE)

    override fun load(): FeedFilterPrefs = FeedFilterPrefs(
        maxDistanceKm = prefs.getInt(KEY_DIST, 0),
        ageMin = prefs.getInt(KEY_AGE_MIN, 18),
        ageMax = prefs.getInt(KEY_AGE_MAX, 99),
        gender = prefs.getString(KEY_GENDER, null)?.takeIf { it.isNotBlank() },
        interests = prefs.getStringSet(KEY_INTERESTS, emptySet())?.toList() ?: emptyList(),
    )

    override fun save(p: FeedFilterPrefs) {
        prefs.edit()
            .putInt(KEY_DIST, p.maxDistanceKm)
            .putInt(KEY_AGE_MIN, p.ageMin)
            .putInt(KEY_AGE_MAX, p.ageMax)
            .putString(KEY_GENDER, p.gender ?: "")
            .putStringSet(KEY_INTERESTS, p.interests.toSet())
            .apply()
    }

    private companion object {
        const val KEY_DIST = "dist_km"
        const val KEY_AGE_MIN = "age_min"
        const val KEY_AGE_MAX = "age_max"
        const val KEY_GENDER = "gender"
        const val KEY_INTERESTS = "interests"
    }
}
