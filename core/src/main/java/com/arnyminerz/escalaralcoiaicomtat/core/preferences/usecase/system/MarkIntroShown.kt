package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.SystemPreferencesRepository

class MarkIntroShown(
    private val systemPreferencesRepository: SystemPreferencesRepository
) {
    suspend operator fun invoke() =
        systemPreferencesRepository.markIntroAsShown()
}
