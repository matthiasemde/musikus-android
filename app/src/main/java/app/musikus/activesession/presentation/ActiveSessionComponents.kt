/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2025 Michael Prommersberger, Matthias Emde
 */
package app.musikus.activesession.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.musikus.R
import app.musikus.core.presentation.components.DialogActions
import app.musikus.core.presentation.components.DialogHeader
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.core.presentation.utils.UiText
import app.musikus.core.presentation.utils.getDurationString
import app.musikus.menu.domain.ColorSchemeSelections
import app.musikus.sessions.presentation.RatingBar
import kotlin.time.Duration.Companion.seconds


@Composable
internal fun PracticeTimer(
    uiState: State<ActiveSessionTimerUiState>,
    sessionState: State<ActiveSessionState>,
    modifier: Modifier = Modifier,
    onResumeTimer: () -> Unit,
    screenSizeClass: ScreenSizeClass,
) {
    Column(
        modifier.animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            style = MaterialTheme.typography.displayLarge,
            text = uiState.value.timerText,
            fontWeight = FontWeight.Light,
            fontSize = if (screenSizeClass.height == WindowHeightSizeClass.Compact) 60.sp else 75.sp
        )
        when (sessionState.value) {
            ActiveSessionState.PAUSED -> {
                ElevatedButton(
                    onClick = onResumeTimer,
                    colors = ButtonDefaults.elevatedButtonColors().copy(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircle,
                        contentDescription = stringResource(
                            id = R.string.active_session_timer_subheading_resume
                        )
                    )
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text(text = uiState.value.subHeadingText.asString())
                }
            }

            else -> {
                Text(
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    text = uiState.value.subHeadingText.asString(),
                )
            }
        }
    }
}

@Composable
internal fun CurrentPracticingItem(
    uiState: State<ActiveSessionCurrentItemUiState?>,
    modifier: Modifier = Modifier,
    screenSizeClass: ScreenSizeClass,
) {
    val item = uiState.value

    AnimatedVisibility(
        visible = item != null,
        enter = expandVertically() + fadeIn(animationSpec = keyframes { durationMillis = 200 }),
    ) {
        if (item == null) return@AnimatedVisibility

        val limitedHeight = screenSizeClass.height == WindowHeightSizeClass.Compact

        Surface(
            modifier
                .fillMaxWidth()
                .border(width = 0.5.dp, color = item.color, shape = MaterialTheme.shapes.large),
            color = item.color.copy(alpha = 0.2f),
            shape = MaterialTheme.shapes.large
        ) {
            AnimatedContent(
                targetState = item.name,
                label = "currentPracticingItem",
                transitionSpec = {
                    slideInVertically { -it } togetherWith slideOutVertically { it }
                }
            ) { itemName ->
                Row(
                    modifier = Modifier.height(if (limitedHeight) 42.dp else 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // leading space
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))

                    val textStyle =
                        if (limitedHeight) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleLarge
                        }

                    Text(
                        modifier = Modifier.weight(1f),
                        text = itemName,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.width(MaterialTheme.spacing.small))

                    Text(
                        text = item.durationText,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )

                    // trailing space
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))
                }
            }
        }
    }
}


/**
 * Floating Action Button to select new item for new section.
 */
@Composable
internal fun AddSectionFAB(
    sessionState: State<ActiveSessionState>,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier.padding(MaterialTheme.spacing.large),
        enter = slideInVertically(initialOffsetY = { it * 2 }),
        exit = slideOutVertically(targetOffsetY = { it * 2 }),
    ) {
        val message = stringResource(
            id =
                if (sessionState.value == ActiveSessionState.NOT_STARTED) {
                    R.string.active_session_add_section_fab_before_session
                } else {
                    R.string.active_session_add_section_fab_during_session
                }
        )
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = message) },
            text = { Text(text = message) },
            expanded = true,
        )
    }
}


@Composable
internal fun EndSessionDialog(
    rating: Int,
    comment: String,
    onRatingChanged: (Int) -> Unit,
    onCommentChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column {
                DialogHeader(title = stringResource(id = R.string.active_session_end_session_dialog_title))

                Column(Modifier.padding(horizontal = MaterialTheme.spacing.medium)) {
                    Text(text = stringResource(id = R.string.active_session_end_session_dialog_rating))
                    Spacer(Modifier.height(MaterialTheme.spacing.small))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RatingBar(
                            image = Icons.Default.Star,
                            rating = rating,
                            total = 5,
                            size = 36.dp,
                            onRatingChanged = onRatingChanged,
                        )
                    }
                    Spacer(Modifier.height(MaterialTheme.spacing.large))
                    OutlinedTextField(
                        value = comment,
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.active_session_end_session_dialog_comment)
                            )
                        },
                        onValueChange = onCommentChanged
                    )
                }
                DialogActions(
                    dismissButtonText = stringResource(id = R.string.active_session_end_session_dialog_dismiss),
                    confirmButtonText = stringResource(id = R.string.active_session_end_session_dialog_confirm),
                    onDismissHandler = onDismiss,
                    onConfirmHandler = onConfirm
                )
            }
        }
    }
}


@Composable
internal fun PauseButton(
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Filled.Pause,
            contentDescription = stringResource(id = R.string.active_session_top_bar_pause)
        )
    }
}

/** Previews */


@PreviewLightDark
@Composable
private fun PreviewEndSessionDialog(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme = theme) {
        EndSessionDialog(
            rating = 3,
            comment = "This is a comment for my session for the Previews. :)",
            onConfirm = {},
            onDismiss = {},
            onRatingChanged = {},
            onCommentChanged = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewCurrentItem(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme) {
        CurrentPracticingItem(
            uiState = remember { mutableStateOf(dummyRunningItem) },
            screenSizeClass = ScreenSizeDefaults.Phone
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewPracticeTimer(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme) {
        PracticeTimer(
            uiState = remember {
                mutableStateOf(ActiveSessionTimerUiState(
                        timerText = getDurationString((42 * 60 + 57).seconds, DurationFormat.MS_DIGITAL).toString(),
                        subHeadingText = UiText.StringResource(R.string.active_session_timer_subheading_paused))
                )
            },
            sessionState = remember { mutableStateOf(ActiveSessionState.PAUSED) },
            onResumeTimer = {},
            screenSizeClass = ScreenSizeDefaults.Phone
        )
    }
}