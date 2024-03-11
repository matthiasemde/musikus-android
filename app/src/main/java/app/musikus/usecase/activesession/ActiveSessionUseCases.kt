package app.musikus.usecase.activesession

data class ActiveSessionUseCases(
    val selectItem: SelectItemUseCase,
    val deleteSection: DeleteSectionUseCase,
    val pause: PauseUseCase,
    val resume: ResumeUseCase,
    val getPracticeTime: GetPracticeTimeUseCase,
    val getRunningSection: GetRunningSectionUseCase,
    val getCompletedSections: GetCompletedSectionsUseCase,
    val getOngoingPauseDuration: GetOngoingPauseDurationUseCase,
    val getPausedState: GetPausedStateUseCase,
    val getStartTime: GetStartTimeUseCase,
    val close: CloseSessionUseCase,
    val reset: ResetSessionUseCase,
)