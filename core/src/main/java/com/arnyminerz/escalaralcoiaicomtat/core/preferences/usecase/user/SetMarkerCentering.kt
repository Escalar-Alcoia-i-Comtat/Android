package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.UserPreferencesRepository

class SetMarkerCentering(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean) =
        userPreferencesRepository.setMarkerClickCenteringEnabled(enabled)
}
