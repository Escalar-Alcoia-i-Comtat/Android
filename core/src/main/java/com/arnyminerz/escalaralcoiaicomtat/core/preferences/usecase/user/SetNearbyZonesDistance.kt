package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.UserPreferencesRepository

class SetNearbyZonesDistance(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(distance: Int) =
        userPreferencesRepository.setNearbyZonesDistance(distance)
}