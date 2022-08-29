package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.Keys
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.set
import timber.log.Timber

/**
 * The view model of the intro page, that allows to update the intro shown preference.
 * @author Arnau Mora
 * @since 20211229
 */
class IntroViewModel private constructor(application: Application) : AndroidViewModel(application) {
    init {
        Timber.d("$this::init")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("$this::onCleared")
    }

    fun markIntroAsShown() = set(Keys.shownIntro, true)

    /**
     * The factory for the [IntroViewModel].
     * @author Arnau Mora
     * @since 20211229
     */
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(IntroViewModel::class.java))
                return IntroViewModel(application) as T
            error("Unknown view model class: $modelClass")
        }
    }
}
