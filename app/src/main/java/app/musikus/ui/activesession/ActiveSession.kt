package app.musikus.ui.activesession

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

            if(uiState.isPaused) {
                Dialog(
                    properties = DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    ),
                    onDismissRequest = { }
                )
                {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(percent = 50)
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(MaterialTheme.spacing.large),
                                text = "Pause: ${
                                    getDurationString(
                                        uiState.totalBreakDuration,
                                        DurationFormat.HMS_DIGITAL
                                    )
                                }",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(32.dp))

                        Button(onClick = viewModel::togglePause) {
                            Text("Resume")
                        }
                    }
                }
            }

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

                Spacer(Modifier.padding(MaterialTheme.spacing.small))

                // Sections List
                if (uiState.sections.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        item {
                            val (name, duration) = uiState.sections.first()
                            Row {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = getDurationString(
                                        duration,
                                        DurationFormat.HMS_DIGITAL
                                    ).toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.padding(MaterialTheme.spacing.small))
                            HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        }
                        items(
                            items = uiState.sections.drop(1)
                        ) { (name, duration) ->

                            Row {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = name)

                                Text(text = getDurationString(duration, DurationFormat.HMS_DIGITAL).toString())
                            }
                        }
                    }
                } else {
                    Box (
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.Center),
                            text = "Quotes",
                        )
                    }
                }

                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ){
                    Button(onClick = viewModel::togglePause) {
                        Text(text = "Pause")
                    }

                    Button(onClick = {
                        viewModel.stopSession()
                        navigateUp()
                    }) {
                        Text(text = "Save Session")
                    }

                    Button(onClick = viewModel::startService) {
                        Text("Start Service")
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
        // show a button for each library item
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
