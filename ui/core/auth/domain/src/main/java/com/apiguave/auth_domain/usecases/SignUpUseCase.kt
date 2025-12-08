package com.apiguave.auth_domain.usecases

import arrow.core.Either
import com.apiguave.auth_domain.model.Account
import com.apiguave.auth_domain.repository.AuthRepository

class SignUpUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(account: Account): Either<Throwable, Unit> = Either.catch {
        repository.signUp(account)
    }
}
