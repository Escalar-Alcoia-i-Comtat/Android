package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Used for fetching the alert notifications enabled preference from the [UserPreferencesRepository].
 * @author Arnau Mora
 * @since 20211229
 */
class GetAlertNotificationsEnabled(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> = userPreferencesRepository.alertNotificationsEnabled
}