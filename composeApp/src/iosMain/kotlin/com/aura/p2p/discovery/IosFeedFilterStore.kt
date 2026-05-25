package com.aura.p2p.discovery

/** iOS [FeedFilterStore]: in-memory stub (Aura is Android-primary). */
class IosFeedFilterStore : FeedFilterStore {
    private var prefs = FeedFilterPrefs()
    override fun load(): FeedFilterPrefs = prefs
    override fun save(prefs: FeedFilterPrefs) { this.prefs = prefs }
}
