/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.settings.presentation.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.settings.domain.ColorSchemeSelections
import app.musikus.settings.domain.ThemeSelections
import app.musikus.settings.domain.usecase.UserPreferencesUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppearanceLanguageUiState(
    val currentLanguage: String,
    val languageDialogShowing: Boolean,
)

data class AppearanceThemeUiState(
    val currentTheme: ThemeSelections,
    val themeDialogShowing: Boolean,
)

data class AppearanceColorSchemeUiState(
    val currentColorScheme: ColorSchemeSelections,
    val colorSchemeDialogShowing: Boolean,
)

data class AppearanceUiState(
    val languageUiState: AppearanceLanguageUiState,
    val themeUiState: AppearanceThemeUiState,
    val colorSchemeUiState: AppearanceColorSchemeUiState
)

sealed class AppearanceUiEvent {
    data class ChangeLanguage(val language: String) : AppearanceUiEvent()
    data object ShowLanguageDialog : AppearanceUiEvent()
    data object HideLanguageDialog : AppearanceUiEvent()
    data class ChangeTheme(val theme: ThemeSelections) : AppearanceUiEvent()
    data object ShowThemeDialog : AppearanceUiEvent()
    data object HideThemeDialog : AppearanceUiEvent()
    data class ChangeColorScheme(val colorScheme: ColorSchemeSelections) : AppearanceUiEvent()
    data object ShowColorSchemeDialog : AppearanceUiEvent()
    data object HideColorSchemeDialog : AppearanceUiEvent()
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
    private val _colorSchemeDialogShowing = MutableStateFlow(false)


    /**
     * Imported flows
     */

    private val currentLanguage = MutableStateFlow("System Default")

    private val currentTheme = userPreferencesUseCases.getTheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeSelections.DEFAULT
    )

    private val currentColorScheme = userPreferencesUseCases.getColorScheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ColorSchemeSelections.DEFAULT
    )

    /**
     * Composing the Ui state
     */

    private val languageUiState = combine(
        _languageDialogShowing,
        currentLanguage
    ) { languageDialogShowing, language ->
        AppearanceLanguageUiState(
            currentLanguage = language,
            languageDialogShowing = languageDialogShowing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppearanceLanguageUiState(
            currentLanguage = currentLanguage.value,
            languageDialogShowing = false
        )
    )

    private val themeUiState = combine(
        _themeDialogShowing,
        currentTheme
    ) { themeDialogShowing, theme ->
        AppearanceThemeUiState(
            currentTheme = theme,
            themeDialogShowing = themeDialogShowing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppearanceThemeUiState(
            currentTheme = currentTheme.value,
            themeDialogShowing = false
        )
    )

    private val colorSchemeUiState = combine(
        _colorSchemeDialogShowing,
        currentColorScheme
    ) { colorSchemeDialogShowing, colorScheme ->
        AppearanceColorSchemeUiState(
            currentColorScheme = colorScheme,
            colorSchemeDialogShowing = colorSchemeDialogShowing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppearanceColorSchemeUiState(
            currentColorScheme = currentColorScheme.value,
            colorSchemeDialogShowing = false
        )
    )

    val uiState = combine(
        languageUiState,
        themeUiState,
        colorSchemeUiState
    ) { languageUiState, themeUiState, colorSchemeUiState ->
        AppearanceUiState(
            languageUiState = languageUiState,
            themeUiState = themeUiState,
            colorSchemeUiState = colorSchemeUiState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppearanceUiState(
            languageUiState = languageUiState.value,
            themeUiState = themeUiState.value,
            colorSchemeUiState = colorSchemeUiState.value
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
            is AppearanceUiEvent.ChangeColorScheme -> {
                _colorSchemeDialogShowing.update { false }
                viewModelScope.launch {
                    userPreferencesUseCases.selectColorScheme(event.colorScheme)
                }
            }
            is AppearanceUiEvent.ShowColorSchemeDialog -> {
                _languageDialogShowing.update { false }
                _colorSchemeDialogShowing.update { true }
            }
            is AppearanceUiEvent.HideColorSchemeDialog -> {
                _colorSchemeDialogShowing.update { false }
            }
        }
    }
}