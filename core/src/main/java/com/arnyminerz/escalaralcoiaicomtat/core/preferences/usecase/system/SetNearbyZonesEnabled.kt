package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.UserPreferencesRepository

class SetNearbyZonesEnabled(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean) =
        userPreferencesRepository.setNearbyZonesEnabled(enabled)
}
