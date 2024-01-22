package app.musikus.ui.activesession

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.ui.MainUIEvent
import app.musikus.ui.MainUiState
import app.musikus.utils.DurationFormat
import app.musikus.utils.TimeProvider
import app.musikus.utils.getDurationString

@Composable
fun ActiveSession(
    mainUiState: MainUiState,
    mainEventHandler: (event: MainUIEvent) -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel(),
    timeProvider: TimeProvider,
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold (
        content = { paddingValues ->

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxWidth()
            ) {


                Text(text = getDurationString(uiState.totalSessionDuration, DurationFormat.HMS_DIGITAL).toString())
                Text(text = getDurationString(uiState.totalBreakDuration, DurationFormat.HMS_DIGITAL).toString())

                Button(onClick = viewModel::startTimer) {
                    Text(text = "Start")
                }

                Button(onClick = viewModel::pauseTimer) {
                    Text(text = "Pause")
                }

                LibraryCard(
                    uiState = uiState.libraryUiState,
                    onLibraryItemClicked = viewModel::folderClicked,
                    onFolderClicked = {}
                )

                LazyColumn {
                    items(
                        items = uiState.sections
                    ) { (name, duration) ->
                        Text(text = "$name: ${getDurationString(duration, DurationFormat.HMS_DIGITAL)}")
                    }
                }
            }
        }
    )
}


@Composable
fun LibraryCard(
    uiState: LibraryCardUiState,
    onLibraryItemClicked: (LibraryItem) -> Unit,
    onFolderClicked: (LibraryFolder) -> Unit
) {
    LazyRow {

        items(
            items = uiState.foldersWithItems,
        ) {
            Button(onClick = {
                onFolderClicked(it.folder)
            }) {
                Text(text = "${it.folder.name}: ${it.items.size}")
            }
        }

        items(
            items = uiState.items
        ) { libraryItem ->
            Button(onClick = {
                onLibraryItemClicked(libraryItem)
            }) {
                Text(text = libraryItem.name)
            }
        }
    }
}
