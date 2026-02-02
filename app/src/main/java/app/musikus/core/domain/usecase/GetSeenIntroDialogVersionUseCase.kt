package app.musikus.core.domain.usecase

import app.musikus.core.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.map

class  GetSeenIntroDialogVersionUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    operator fun invoke(featureName: String) = userPreferencesRepository.appIntroSeenDialogVersions.map {
        it[featureName] ?: -1    // return -1 if not seen at all
    }
}