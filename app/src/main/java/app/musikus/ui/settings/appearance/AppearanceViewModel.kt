/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.datastore.ThemeSelections
import app.musikus.usecase.userpreferences.UserPreferencesUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppearanceUiState(
    val currentLanguage: String,
    val languageDialogShowing: Boolean,
    val currentTheme: ThemeSelections,
    val themeDialogShowing: Boolean,
)

sealed class AppearanceUiEvent {
    data class ChangeLanguage(val language: String) : AppearanceUiEvent()
    data object ShowLanguageDialog : AppearanceUiEvent()
    data object HideLanguageDialog : AppearanceUiEvent()
    data class ChangeTheme(val theme: ThemeSelections) : AppearanceUiEvent()
    data object ShowThemeDialog : AppearanceUiEvent()
    data object HideThemeDialog : AppearanceUiEvent()
}

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val userPreferencesUseCases: UserPreferencesUseCases
) : ViewModel() {

    /**
     * Own state flows
     */

    private val _languageDialogShowing = MutableStateFlow(false)
    private val _themeDialogShowing = MutableStateFlow(false)


    /**
     * Imported flows
     */

    private val currentLanguage = MutableStateFlow("System Default")

    private val currentTheme = userPreferencesUseCases.getTheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeSelections.DEFAULT
    )

    /**
     * Composing the Ui state
     */

    val uiState = combine(
        _languageDialogShowing,
        _themeDialogShowing,
        currentLanguage,
        currentTheme
    ) { languageDialogShowing, themeDialogShowing, language, theme ->
        AppearanceUiState(
            currentLanguage = language,
            languageDialogShowing = languageDialogShowing,
            currentTheme = theme,
            themeDialogShowing = themeDialogShowing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppearanceUiState(
            currentLanguage = currentLanguage.value,
            languageDialogShowing = false,
            currentTheme = currentTheme.value,
            themeDialogShowing = false
        )
    )

    fun onUiEvent(event: AppearanceUiEvent) {
        when(event) {
            is AppearanceUiEvent.ChangeLanguage -> {
//                viewModelScope.launch {
//                    userPreferencesUseCases.setLanguage(event.language)
//                }
            }
            is AppearanceUiEvent.ShowLanguageDialog -> {
                _themeDialogShowing.update { false }
                _languageDialogShowing.update { true }
            }
            is AppearanceUiEvent.HideLanguageDialog -> {
                _languageDialogShowing.update { false }
            }
            is AppearanceUiEvent.ChangeTheme -> {
                _themeDialogShowing.update { false }
                viewModelScope.launch {
                    userPreferencesUseCases.selectTheme(event.theme)
                }
            }
            is AppearanceUiEvent.ShowThemeDialog -> {
                _languageDialogShowing.update { false }
                _themeDialogShowing.update { true }
            }
            is AppearanceUiEvent.HideThemeDialog -> {
                _themeDialogShowing.update { false }
            }
        }
    }
}