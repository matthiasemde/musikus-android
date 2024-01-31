package app.musikus.usecase.library

import app.musikus.repository.LibraryRepository

class GetAllLibraryItemsUseCase(
    private val libraryRepository: LibraryRepository,
) {

    operator fun invoke() = libraryRepository.items
}