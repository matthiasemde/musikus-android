package app.musikus.core.domain.usecase

import app.musikus.core.domain.UserPreferencesRepository

class SetSeenIntroDialogUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    suspend operator fun invoke(featureName: String, version: Int) {
        userPreferencesRepository.updateAppIntroSeenDialogVersion( featureName, version)
    }
}