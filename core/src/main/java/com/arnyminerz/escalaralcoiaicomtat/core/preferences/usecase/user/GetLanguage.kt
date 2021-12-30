package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Used for fetching the language preference from the [UserPreferencesRepository].
 * @author Arnau Mora
 * @since 20211229
 */
class GetLanguage(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<String> = userPreferencesRepository.language
}