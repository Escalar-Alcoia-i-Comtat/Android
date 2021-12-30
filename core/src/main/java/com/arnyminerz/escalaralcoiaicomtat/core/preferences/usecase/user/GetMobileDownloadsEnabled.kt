package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Used for fetching the mobile data downloads enabled preference from the [UserPreferencesRepository].
 * @author Arnau Mora
 * @since 20211229
 */
class GetMobileDownloadsEnabled(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> = userPreferencesRepository.mobileDownloadsEnabled
}