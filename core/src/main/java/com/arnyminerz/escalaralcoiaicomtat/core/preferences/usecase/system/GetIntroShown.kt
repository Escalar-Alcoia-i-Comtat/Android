package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.SystemPreferencesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Used for fetching the intro shown preference from the [SystemPreferencesRepository].
 * @author Arnau Mora
 * @since 20211229
 */
class GetIntroShown(
    private val systemPreferencesRepository: SystemPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> = systemPreferencesRepository.shownIntro
}
