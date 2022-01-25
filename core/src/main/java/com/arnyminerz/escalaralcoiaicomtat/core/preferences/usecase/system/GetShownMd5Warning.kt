package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.SystemPreferencesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Used for fetching the shown MD5 warning preference from the [SystemPreferencesRepository].
 * @author Arnau Mora
 * @since 20211229
 */
class GetShownMd5Warning(
    private val systemPreferencesRepository: SystemPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> = systemPreferencesRepository.shownMd5Warning
}
