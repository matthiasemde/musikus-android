package app.musikus.library.domain.usecase

import app.musikus.library.domain.LibraryRepository

class GetAllLibraryItemsUseCase(
    private val libraryRepository: LibraryRepository,
) {

    operator fun invoke() = libraryRepository.items
}