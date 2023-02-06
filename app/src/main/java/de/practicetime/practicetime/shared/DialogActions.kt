package de.practicetime.practicetime.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.practicetime.practicetime.R

@Composable
fun DialogActions(
    onDismissHandler: () -> Unit,
    onConfirmHandler: () -> Unit,
    confirmButtonText: String = stringResource(id = R.string.dialogConfirm),
    dismissButtonText: String = stringResource(id = R.string.dialogDismiss),
    confirmButtonEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onDismissHandler,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = dismissButtonText)
        }
        TextButton(
            onClick = onConfirmHandler,
            enabled = confirmButtonEnabled,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = confirmButtonText)
        }
    }
}