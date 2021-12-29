package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.UserPreferencesRepository

class SetLanguage(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(language: String) =
        userPreferencesRepository.setLanguage(language)
}