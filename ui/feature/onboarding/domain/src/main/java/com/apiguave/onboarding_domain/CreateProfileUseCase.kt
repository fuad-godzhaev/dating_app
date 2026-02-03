package com.apiguave.onboarding_domain

import com.apiguave.auth_domain.repository.AuthRepository
import com.apiguave.picture_domain.repository.PictureRepository
import com.apiguave.profile_domain.model.Gender
import com.apiguave.profile_domain.model.Orientation
import com.apiguave.profile_domain.repository.ProfileRepository
import java.time.LocalDate


class CreateProfileUseCase(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val pictureRepository: PictureRepository
) {

    suspend operator fun invoke(
        name: String,
        birthdate: LocalDate,
        bio: String,
        gender: Gender,
        orientation: Orientation,
        pictures: List<String>
    ): Result<Unit> {
        return Result.runCatching {
            val userId = authRepository.userId
            profileRepository.addProfile(userId!!, name, birthdate, bio, gender, orientation)

            // Try to upload pictures, but don't fail profile creation if it fails
            // (Firebase Storage not available in AT Protocol backend)
            try {
                if (pictures.size >= 1) {
                    val pictureNames = pictureRepository.addPictures(pictures)
                    profileRepository.updatePictures(pictureNames)
                } else {
                    // No pictures to upload
                    profileRepository.updatePictures(emptyList())
                }
            } catch (e: Exception) {
                // Log picture upload failure but continue with profile creation
                //android.util.Log.w("CreateProfileUseCase", "Picture upload failed (Firebase not configured): ${e.message}")
                // Update profile with empty pictures for now
                profileRepository.updatePictures(emptyList())
            }
        }
    }
}
