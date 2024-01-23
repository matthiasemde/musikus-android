package app.musikus.usecase.userpreferences

data class UserPreferencesUseCases (
    val getFolderSortInfo: GetFolderSortInfoUseCase,
    val getItemSortInfo: GetItemSortInfoUseCase,
    val getGoalSortInfo: GetGoalSortInfoUseCase,
    val selectFolderSortMode: SelectFolderSortModeUseCase,
    val selectItemSortMode: SelectItemSortModeUseCase,
    val selectGoalSortMode: SelectGoalsSortModeUseCase
)