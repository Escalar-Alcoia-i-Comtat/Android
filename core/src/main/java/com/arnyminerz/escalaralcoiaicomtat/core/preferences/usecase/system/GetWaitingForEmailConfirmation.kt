package com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.SystemPreferencesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Used for fetching the waiting for email confirmation preference from the [SystemPreferencesRepository].
 * @author Arnau Mora
 * @since 20220118
 */
class GetWaitingForEmailConfirmation(
    private val systemPreferencesRepository: SystemPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> = systemPreferencesRepository.waitingForEmailConfirmation
}
