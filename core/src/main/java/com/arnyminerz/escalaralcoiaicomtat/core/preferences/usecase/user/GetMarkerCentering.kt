package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Used for fetching the center marker on click preference from the [UserPreferencesRepository].
 * @author Arnau Mora
 * @since 20211229
 */
class GetMarkerCentering(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> = userPreferencesRepository.markerClickCenteringEnabled
}