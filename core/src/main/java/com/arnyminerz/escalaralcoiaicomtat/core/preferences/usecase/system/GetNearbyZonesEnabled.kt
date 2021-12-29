package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Used for fetching the nearby zones enabled preference from the [UserPreferencesRepository].
 * @author Arnau Mora
 * @since 20211229
 */
class GetNearbyZonesEnabled(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> = userPreferencesRepository.nearbyZonesEnabled
}
