package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Used for fetching the nearby zones distance preference from the [UserPreferencesRepository].
 * @author Arnau Mora
 * @since 20211229
 */
class GetNearbyZonesDistance(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<Int> = userPreferencesRepository.nearbyZonesDistance
}
