package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.MarkIntroShown
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The view model of the intro page, that allows to update the intro shown preference.
 * @author Arnau Mora
 * @since 20211229
 * @param _markIntroShown The [MarkIntroShown] instance for marking the intro as shown.
 */
class IntroViewModel private constructor(
    private val _markIntroShown: MarkIntroShown
) : ViewModel() {
    init {
        Timber.d("$this::init")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("$this::onCleared")
    }

    fun markIntroAsShown() {
        viewModelScope.launch { _markIntroShown() }
    }

    /**
     * The factory for the [IntroViewModel].
     * @author Arnau Mora
     * @since 20211229
     * @param markIntroShown The [MarkIntroShown] instance to update the intro shown preference.
     */
    class Factory(
        private val markIntroShown: MarkIntroShown
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(IntroViewModel::class.java)) {
                IntroViewModel(markIntroShown) as T
            }
            error("Unknown view model class: $modelClass")
        }
    }
}

val PreferencesModule.introViewModelFactory
    get() = IntroViewModel.Factory(this.markIntroShown)

private val PreferencesModule.markIntroShown
    get() = MarkIntroShown(systemPreferencesRepository)
