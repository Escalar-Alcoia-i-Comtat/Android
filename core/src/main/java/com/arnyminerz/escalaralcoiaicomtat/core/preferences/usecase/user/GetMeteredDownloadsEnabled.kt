package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Used for fetching the metered data downloads enabled preference from the [UserPreferencesRepository].
 * @author Arnau Mora
 * @since 20211229
 */
class GetMeteredDownloadsEnabled(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> = userPreferencesRepository.meteredDownloadsEnabled
}