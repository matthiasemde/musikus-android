package app.musikus.ui.activesession

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.spacing
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
    navigateUp: () -> Unit
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


                // Practice Timer
                Text(
                    style = MaterialTheme.typography.displayMedium,
                    text = getDurationString(uiState.totalSessionDuration, DurationFormat.HMS_DIGITAL).toString()
                )
                Text(text = "Practice time")



                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                // Library Items
                LibraryCard(
                    uiState = uiState.libraryUiState,
                    onLibraryItemClicked = viewModel::itemClicked,
                    onFolderClicked = {}
                )

                // Sections List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(
                        items = uiState.sections
                    ) { (name, duration) ->
                        Text(text = "$name: ${getDurationString(duration, DurationFormat.HMS_DIGITAL)}")
                    }
                }


                Row {
                    Button(onClick = viewModel::startTimer) {
                        Text(text = "Start")
                    }

                    Button(onClick = viewModel::pauseTimer) {
                        Text(text = "Pause")
                    }


                    Button(onClick = {
                        viewModel.stopSession()
                        navigateUp()
                    }) {
                        Text(text = "Stop")
                    }
                }




            }
        }
    )
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryCard(
    uiState: LibraryCardUiState,
    onLibraryItemClicked: (LibraryItem) -> Unit,
    onFolderClicked: (LibraryFolder) -> Unit,
) {
    FlowColumn(
        maxItemsInEachColumn = 3,
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        for(libraryItem in (uiState.items + uiState.foldersWithItems.flatMap { it.items })) {
            Button(
                modifier = Modifier
                    .height(50.dp)
                    .width(200.dp),
                onClick = {
                    onLibraryItemClicked(libraryItem)
                }) {
                Text(text = libraryItem.name)
            }
        }
    }
}
