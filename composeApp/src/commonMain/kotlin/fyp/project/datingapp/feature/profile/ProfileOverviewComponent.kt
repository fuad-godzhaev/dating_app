package fyp.project.datingapp.feature.profile

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.domain.auth.AuthRepository
import fyp.project.datingapp.records.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Self profile overview (AURA_DESIGN_SPEC §6.10). */
interface ProfileOverviewComponent {
    val state: Value<State>
    fun onSettings()
    fun onEditProfile()
    fun onSignOut()
    fun onBack()

    data class State(
        val profile: UserProfile? = null,
        val did: String? = null,
        val signingOut: Boolean = false,
    )
}

class DefaultProfileOverviewComponent(
    componentContext: ComponentContext,
    private val repositoryManager: RepositoryManager,
    private val authRepository: AuthRepository,
    private val onSettingsClick: () -> Unit,
    private val onEditProfileClick: () -> Unit,
    private val onSignedOut: () -> Unit,
    private val onBackClick: () -> Unit,
) : ProfileOverviewComponent, ComponentContext by componentContext {

    private val _state = MutableValue(ProfileOverviewComponent.State())
    override val state: Value<ProfileOverviewComponent.State> = _state

    private val scope = coroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            val profile = runCatching { repositoryManager.getMyProfile() }.getOrNull()
            val did = runCatching { authRepository.getDid() }.getOrNull()
            _state.value = _state.value.copy(profile = profile, did = did)
        }
    }

    override fun onSettings() = onSettingsClick()
    override fun onEditProfile() = onEditProfileClick()
    override fun onBack() = onBackClick()

    override fun onSignOut() {
        if (_state.value.signingOut) return
        _state.value = _state.value.copy(signingOut = true)
        scope.launch {
            // TODO(B): also peerProfileFeed.stop() + cancel FGS/WorkManager before deleting.
            runCatching { authRepository.deleteAccount() }
            onSignedOut()
        }
    }
}
