package fyp.project.datingapp.feature.profile

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.records.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Edit-profile screen (AURA_DESIGN_SPEC §6.11). */
interface EditProfileComponent {
    val state: Value<State>
    fun onDisplayNameChanged(value: String)
    fun onBioChanged(value: String)
    fun onAgeChanged(value: Int)
    fun onInterestsChanged(value: List<String>)
    fun onSave()
    fun onBack()

    data class State(
        val loaded: UserProfile? = null,
        val displayName: String = "",
        val bio: String = "",
        val age: Int = 18,
        val interests: List<String> = emptyList(),
        val isSaving: Boolean = false,
        val error: String? = null,
    ) {
        val canSave: Boolean get() = loaded != null && displayName.isNotBlank() && age >= 18
    }
}

class DefaultEditProfileComponent(
    componentContext: ComponentContext,
    private val repositoryManager: RepositoryManager,
    private val onSaved: () -> Unit,
    private val onBackClick: () -> Unit,
) : EditProfileComponent, ComponentContext by componentContext {

    private val _state = MutableValue(EditProfileComponent.State())
    override val state: Value<EditProfileComponent.State> = _state

    private val scope = coroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            val p = runCatching { repositoryManager.getMyProfile() }.getOrNull()
            if (p != null) {
                _state.value = _state.value.copy(
                    loaded = p,
                    displayName = p.displayName,
                    bio = p.bio ?: "",
                    age = p.age,
                    interests = p.interests,
                )
            }
        }
    }

    override fun onDisplayNameChanged(value: String) { _state.value = _state.value.copy(displayName = value) }
    override fun onBioChanged(value: String) { _state.value = _state.value.copy(bio = value) }
    override fun onAgeChanged(value: Int) { _state.value = _state.value.copy(age = value) }
    override fun onInterestsChanged(value: List<String>) { _state.value = _state.value.copy(interests = value) }
    override fun onBack() = onBackClick()

    override fun onSave() {
        val current = _state.value
        val base = current.loaded ?: return
        if (!current.canSave || current.isSaving) return
        _state.value = current.copy(isSaving = true, error = null)
        scope.launch {
            val updated = base.copy(
                displayName = current.displayName,
                bio = current.bio,
                age = current.age,
                interests = current.interests,
            )
            repositoryManager.putProfile(updated)
                .onSuccess {
                    _state.value = _state.value.copy(isSaving = false)
                    onSaved()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Failed to save")
                }
        }
    }
}
