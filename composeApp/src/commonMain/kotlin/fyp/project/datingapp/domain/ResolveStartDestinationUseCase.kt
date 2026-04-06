package fyp.project.datingapp.domain

import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.domain.auth.AuthRepository

enum class StartDestination { SignIn, SignUp }

class ResolveStartDestinationUseCase(
    private val auth: AuthRepository,
    private val repositoryManager: RepositoryManager
) {
    suspend operator fun invoke(): Result<StartDestination> = runCatching {
        val hasIdentity = auth.hasIdentity()
        val hasPin = auth.hasPin()
        val did = auth.getDid()
        val hasProfile = did != null && repositoryManager.hasProfile(did)

        if (hasIdentity && hasPin && hasProfile) {
            StartDestination.SignIn
        } else {
            StartDestination.SignUp
        }
    }
}
