package com.apiguave.auth_domain.usecases

import com.apiguave.auth_domain.model.Account
import com.apiguave.auth_domain.repository.AuthRepository

class SignUpUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(account: Account): Result<Unit> {
        return Result.runCatching {
            authRepository.signUp(account)
        }
    }
}
