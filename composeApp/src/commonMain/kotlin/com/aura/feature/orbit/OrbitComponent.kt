package com.aura.feature.orbit

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.aura.database.appView.dao.IncomingLikesDao
import com.aura.p2p.fetch.ProfileFetcher
import com.aura.p2p.like.LikeService
import com.aura.records.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Orbit / "liked you" grid (AURA_DESIGN_SPEC §6.18): the people who liked this user but
 * aren't matched yet. Each liker's profile (name/age) is resolved best-effort over the
 * fetch cascade; unresolved likers render as a placeholder card. Tapping a card likes
 * back - a reciprocal like creates the match, which removes the card from the unmatched
 * feed.
 */
interface OrbitComponent {
    val state: Value<State>
    fun onLikeBack(did: String, name: String?)
    fun onBack()

    data class State(val cards: List<Card> = emptyList())
    data class Card(val did: String, val name: String?, val age: Int?)
}

class DefaultOrbitComponent(
    componentContext: ComponentContext,
    private val incomingLikes: IncomingLikesDao,
    private val fetcher: ProfileFetcher,
    private val likeService: LikeService,
    private val onBackClick: () -> Unit,
) : OrbitComponent, ComponentContext by componentContext {

    private val _state = MutableValue(OrbitComponent.State())
    override val state: Value<OrbitComponent.State> = _state

    private val scope = coroutineScope(Dispatchers.Main)

    // did -> resolved profile (absent = not tried; null value = tried and failed/offline).
    private val resolved = mutableMapOf<String, UserProfile?>()

    init {
        scope.launch {
            incomingLikes.getUnmatchedLikes().collect { likes ->
                _state.value = OrbitComponent.State(likes.map { cardFor(it.fromDid) })
                likes.forEach { like ->
                    if (!resolved.containsKey(like.fromDid)) {
                        resolved[like.fromDid] = null // mark in-flight to avoid duplicate fetches
                        scope.launch {
                            val profile = runCatching { fetcher.fetch(like.fromDid) }.getOrNull()
                            resolved[like.fromDid] = profile
                            _state.value = OrbitComponent.State(
                                _state.value.cards.map { if (it.did == like.fromDid) cardFor(it.did) else it },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun cardFor(did: String): OrbitComponent.Card {
        val profile = resolved[did]
        return OrbitComponent.Card(did = did, name = profile?.displayName, age = profile?.age)
    }

    override fun onLikeBack(did: String, name: String?) {
        scope.launch { runCatching { likeService.sendLike(did, name) } }
    }

    override fun onBack() = onBackClick()
}
