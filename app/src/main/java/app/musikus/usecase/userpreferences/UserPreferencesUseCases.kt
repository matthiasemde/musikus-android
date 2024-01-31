package app.musikus.usecase.userpreferences

data class UserPreferencesUseCases (
    val getTheme: GetThemeUseCase,
    val getFolderSortInfo: GetFolderSortInfoUseCase,
    val getItemSortInfo: GetItemSortInfoUseCase,
    val getGoalSortInfo: GetGoalSortInfoUseCase,
    val selectTheme: SelectThemeUseCase,
    val selectFolderSortMode: SelectFolderSortModeUseCase,
    val selectItemSortMode: SelectItemSortModeUseCase,
    val selectGoalSortMode: SelectGoalsSortModeUseCase,
    val getMetronomeSettings: GetMetronomeSettingsUseCase,
    val changeMetronomeSettings: ChangeMetronomeSettingsUseCase
)