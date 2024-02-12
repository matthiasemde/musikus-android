package app.musikus.ui.settings.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.musikus.BuildConfig
import app.musikus.R
import app.musikus.ui.components.conditional
import app.musikus.ui.theme.spacing

data class AboutScreenItem (
    val firstLine: String? = null,
    val secondLine: String? = null,
    val onClick: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        var showLicenses by remember { mutableStateOf(false) }

        val aboutScreenItems = listOf(
            listOf(AboutScreenItem(
                firstLine = stringResource(id = R.string.development_title),
                secondLine = stringResource(id = R.string.development_text)
            )),
            listOf(AboutScreenItem(
                firstLine = "Publisher",
                secondLine = "Matthias Emde\nConnollystraße 25\n80809 Munich, Germany\ncontact@musikus.app",
            )),
            listOf(
                AboutScreenItem(
                    firstLine = "Version",
                    secondLine = BuildConfig.VERSION_NAME
                ),
                AboutScreenItem(
                    firstLine = "Licenses",
                    onClick = { showLicenses = true }
                ),
                AboutScreenItem(
                    secondLine =
                        "Copyright Matthias Emde, Michael Prommersberger\n" +
                        "Licensed under the Mozilla Public License Version 2.0"
                )
            ),
        )

        Column(
            modifier = Modifier.padding(paddingValues),
        ) {

            for (group in aboutScreenItems) {
                for (item in group) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .conditional(item.onClick != null) {
                                clickable { item.onClick?.invoke() }
                            }
                            .padding(
                                horizontal = MaterialTheme.spacing.large,
                                vertical = MaterialTheme.spacing.medium
                            ),
                    ) {
                        Column {
                            item.firstLine?.let {
                                Text(text = it)
                            }
                            item.secondLine?.let {
                                Text(
                                    text = it,
                                    style = LocalTextStyle.current.copy(
                                        fontSize = LocalTextStyle.current.fontSize * 0.9f,
                                        color = LocalContentColor.current.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                    }
                }
                if (group != aboutScreenItems.last()) {
                    HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.medium))
                }
            }
        }

        if(showLicenses) {
            Dialog(
                onDismissRequest = { showLicenses = false },
                properties = DialogProperties(dismissOnClickOutside = true)
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(MaterialTheme.spacing.medium)) {
                        Text(text = "Licenses")
                        TextButton(onClick = { showLicenses = false }) {
                            Text(text = "Cancel")
                        }
                    }
                }
            }
        }
    }
}