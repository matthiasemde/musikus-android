package app.musikus.usecase.library

import app.musikus.database.Nullable
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.repository.FakeLibraryRepository
import app.musikus.repository.FakeUserPreferencesRepository
import app.musikus.utils.LibraryItemSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.google.common.truth.Truth.assertThat


class GetItemsUseCaseTest {
    private lateinit var getItems: GetItemsUseCase
    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    @BeforeEach
    fun setUp() {
        fakeLibraryRepository = FakeLibraryRepository()
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
        getItems = GetItemsUseCase(
            libraryRepository = fakeLibraryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
        )

        val itemCreationAttributes = ('a'..'z').mapIndexed { index, name ->
            LibraryItemCreationAttributes(
                name = name.toString(),
                libraryFolderId = Nullable(null),
                colorIndex = index % 10
            )
        }

        val folderCreationAttributes = LibraryFolderCreationAttributes("Test")

        runBlocking {
            fakeLibraryRepository.addFolder(folderCreationAttributes)
            val folderId = fakeLibraryRepository.folders.first().first().folder.id
            itemCreationAttributes.shuffled().forEach {
                fakeLibraryRepository.addItem(it)
                fakeLibraryRepository.addItem(it.copy(
                    libraryFolderId = Nullable(folderId)
                ))
            }
        }
    }

    private fun testItemSorting(
        sortMode: LibraryItemSortMode,
        sortDirection: SortDirection,
    ) {
        runBlocking {
            fakeUserPreferencesRepository.updateLibraryItemSortInfo(
                sortInfo = SortInfo(
                    mode = sortMode,
                    direction = sortDirection
                )
            )
            val items = getItems(folderId = Nullable(null)).first()
            when (sortDirection) {
                SortDirection.ASCENDING ->
                    assertThat(items).isInOrder(sortMode.comparator)
                SortDirection.DESCENDING ->
                    assertThat(items).isInOrder(sortMode.comparator.reversed())
            }
        }
    }

    @Test
    fun `Get items, items are sorted by date added descending`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.DATE_ADDED,
            sortDirection = SortDirection.DESCENDING
        )
    }

    @Test
    fun `Set item sort mode to date added ascending then get items, items are sorted by date ascending`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.DATE_ADDED,
            sortDirection = SortDirection.ASCENDING
        )
    }

    @Test
    fun `Set item sort mode to name descending then get items, items are sorted by name descending`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.NAME,
            sortDirection = SortDirection.DESCENDING
        )
    }

    @Test
    fun `Set item sort mode to name ascending then get items, items are sorted by name ascending`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.NAME,
            sortDirection = SortDirection.ASCENDING
        )
    }

    @Test
    fun `Set item sort mode to last modified descending then get items, items are sorted by last modified descending`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.LAST_MODIFIED,
            sortDirection = SortDirection.DESCENDING
        )
    }

    @Test
    fun `Set item sort mode to last modified ascending then get items, items are sorted by last modified ascending`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.LAST_MODIFIED,
            sortDirection = SortDirection.ASCENDING
        )
    }

    @Test
    fun `Set item sort mode to color descending then get items, items are sorted by color descending`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.COLOR,
            sortDirection = SortDirection.DESCENDING
        )
    }

    @Test
    fun `Set item sort mode to color ascending then get items, items are sorted by color ascending`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.COLOR,
            sortDirection = SortDirection.ASCENDING
        )
    }

    @Test
    fun `Set item sort mode to custom then get items, items are sorted by custom order`() {
        // TODO
    }
}