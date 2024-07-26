package app.musikus.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.musikus.R
import app.musikus.settings.domain.ColorSchemeSelections
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.spacing

@Composable
fun DialogActions(
    onDismissHandler: () -> Unit,
    onConfirmHandler: () -> Unit,
    confirmButtonText: String = stringResource(id = android.R.string.ok),
    dismissButtonText: String = stringResource(id = android.R.string.cancel),
    confirmButtonEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .padding(MaterialTheme.spacing.large)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            modifier = Modifier.semantics {
                contentDescription = dismissButtonText
            },
            onClick = onDismissHandler,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = dismissButtonText)
        }
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        Button(
            modifier = Modifier.semantics {
                contentDescription = confirmButtonText
            },
            onClick = onConfirmHandler,
            enabled = confirmButtonEnabled
        ) {
            Text(text = confirmButtonText, softWrap = false)
        }
    }
}

@MusikusPreviewElement1
@Composable
private fun PreviewDialogActions(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections
) {
    MusikusThemedPreview(theme = theme) {
        DialogActions(
            onDismissHandler = {},
            onConfirmHandler = {}
        )
    }
}