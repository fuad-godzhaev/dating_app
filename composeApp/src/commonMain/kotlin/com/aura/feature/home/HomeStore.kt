package com.aura.feature.home

import com.arkivanov.mvikotlin.core.store.Store
import com.aura.feature.home.HomeStore.Intent
import com.aura.feature.home.HomeStore.State
import com.aura.p2p.discovery.FeedFilterPrefs
import com.aura.records.Match
import com.aura.records.UserProfile

interface HomeStore: Store<Intent, State, HomeStore.Label>{
    sealed class Intent {
        data object LoadProfiles : Intent()
        data class ProfileLiked(val id: String) : Intent()
        data class ProfileSwiped(val id: String) : Intent()
        data object DismissProfile : Intent()
        data class ProfileExtended(val id: String) : Intent()
        data object OpenedMessages : Intent()
        data object OpenedProfile : Intent()
        data class MatchOccured(val text: String) : Intent()
        data object DismissDialog : Intent()
        // Feed filters (edited from the Home screen): open/close the sheet, apply new prefs.
        data object OpenFilters : Intent()
        data object CloseFilters : Intent()
        data class ApplyFilters(val filters: FeedFilterPrefs) : Intent()
    }

    data class State(
        val contentState: ContentState = ContentState.Loading,
        val dialog: MatchDialogState? = null,
        val filters: FeedFilterPrefs = FeedFilterPrefs(),
        val filterSheetOpen: Boolean = false,
        // Local path to the user's own first photo, shown on the top-bar avatar.
        val selfPhotoPath: String? = null,
    ) {
        sealed interface ContentState {
            data object Loading : ContentState
            data class Loaded(val profiles: List<ProfileCardState>) : ContentState
            data class Error(val message: String) : ContentState
        }

        data class ProfileCardState(
            val profile: UserProfile,
            val pictureBlobs: List<PictureState>
        )

        sealed interface PictureState {
            data class Loading(val ref: String) : PictureState
            // filePath = the on-disk location of the fetched blob (Part 3); the
            // card view decodes it lazily. ref is the blob CID, kept for keying.
            data class Loaded(val ref: String, val filePath: String) : PictureState
        }

        data class MatchDialogState(
            val match: Match,
            val matchDialog: String,
            val pictureBlobs: List<PictureState>
        )
    }

    sealed interface Label {
        data class Error(val message: String) : Label
    }
}