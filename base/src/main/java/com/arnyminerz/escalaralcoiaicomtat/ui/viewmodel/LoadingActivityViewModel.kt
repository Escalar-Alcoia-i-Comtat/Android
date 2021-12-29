package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.GetIntroShown
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

class LoadingActivityViewModel(
    introShown: GetIntroShown
) : ViewModel() {
    init {
        Timber.d("$this::init")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("$this::onCleared")
    }

    val introShown: StateFlow<Boolean> = introShown().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false
    )

    /**
     * The factory for the [LoadingActivityViewModel].
     * @author Arnau Mora
     * @since 20211229
     */
    class Factory(
        private val introShown: GetIntroShown
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoadingActivityViewModel::class.java)) {
                return LoadingActivityViewModel(introShown) as T
            }
            error("Unknown view model class: $modelClass")
        }
    }
}

/**
 * Returns the factory for the loading activity view model.
 * @author Arnau Mora
 * @since 20211229
 */
val PreferencesModule.loadingActivityViewModel
    get() = LoadingActivityViewModel.Factory(this.introShown)
