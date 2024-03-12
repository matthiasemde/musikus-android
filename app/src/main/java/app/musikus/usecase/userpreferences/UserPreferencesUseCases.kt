package app.musikus.usecase.userpreferences

data class UserPreferencesUseCases (
    val getTheme: GetThemeUseCase,
    val getColorScheme: GetColorSchemeUseCase,
    val getFolderSortInfo: GetFolderSortInfoUseCase,
    val getItemSortInfo: GetItemSortInfoUseCase,
    val getGoalSortInfo: GetGoalSortInfoUseCase,
    val selectTheme: SelectThemeUseCase,
    val selectColorScheme: SelectColorSchemeUseCase,
    val selectFolderSortMode: SelectFolderSortModeUseCase,
    val selectItemSortMode: SelectItemSortModeUseCase,
    val selectGoalSortMode: SelectGoalsSortModeUseCase,
    val getMetronomeSettings: GetMetronomeSettingsUseCase,
    val changeMetronomeSettings: ChangeMetronomeSettingsUseCase
)