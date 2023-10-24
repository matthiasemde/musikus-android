package app.musikus.ui.sessionlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.musikus.Musikus
import app.musikus.utils.SCALE_FACTOR_FOR_SMALL_TEXT
import app.musikus.utils.TIME_FORMAT_HUMAN_PRETTY
import app.musikus.utils.getDurationString
import app.musikus.viewmodel.EditSessionViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSession(
    editSessionViewModel: EditSessionViewModel = viewModel(),
    sessionToEditId: UUID,
) {
    val editSessionUiState by editSessionViewModel.editSessionUiState.collectAsState()

    editSessionViewModel.setSessionToEditId(sessionToEditId)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()


    Scaffold(
//        contentWindowInsets = WindowInsets(bottom = 0.dp),
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Edit Session") },
                navigationIcon = {
                    IconButton(onClick = editSessionViewModel::onTopBarBackPressed) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { innerPadding ->
            val contentUiState = editSessionUiState.contentUiState
            val sessionEditData = contentUiState.sessionEditData
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = "Your rating"
                )
                RatingBar(
                    image = Icons.Default.Star,
                    rating = sessionEditData.rating,
                    total = 5,
                    size = 24.dp,
                    onRatingChanged = editSessionViewModel::onRatingChanged,
                )
                Text(text = "Your session time")
                sessionEditData.sections.forEach { sectionWithLibraryItem ->
                    Row {
                        Row(
                            modifier = Modifier
                                .height(IntrinsicSize.Min)
                                .width(0.dp)
                                .weight(4f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(end = 8.dp)
                                    .width(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Color(
                                            Musikus.getLibraryItemColors(LocalContext.current)[sectionWithLibraryItem.libraryItem.colorIndex]
                                        )
                                    )
                            )
                            Text(
                                text = sectionWithLibraryItem.libraryItem.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            modifier = Modifier
                                .width(0.dp)
                                .weight(2f),
                            text = getDurationString(
                                sectionWithLibraryItem.section.duration,
                                TIME_FORMAT_HUMAN_PRETTY,
                                SCALE_FACTOR_FOR_SMALL_TEXT
                            ).toString(),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(text = "Your comment")
                OutlinedTextField(
                    value = sessionEditData.comment,
                    onValueChange = editSessionViewModel::onCommentChanged,
                )
            }
        },
        bottomBar = {
            BottomAppBar(
                content = {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = editSessionViewModel::onCancelHandler,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Cancel")
                    }
                    TextButton(
                        modifier = Modifier.padding(end = 16.dp),
                        onClick = editSessionViewModel::onConfirmHandler,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text =  "Edit")
                    }
                }
            )
        },
    )
}