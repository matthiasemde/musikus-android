/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 *
 */


package app.musikus.ui.activesession

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.musikus.ui.theme.dimensions


@Composable
fun FeatureCard(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    extendedContentComposable: (@Composable () ->  Unit)? = null,
    contentComposable: @Composable () -> Unit,
) {
    val cornerRadius = MaterialTheme.dimensions.featureCardCornerRadius


    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MaterialTheme.dimensions.featureCardMargin,
                end = MaterialTheme.dimensions.featureCardMargin,
                bottom = MaterialTheme.dimensions.featureCardMargin
            ),
        shape = RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius,
        ),
        shadowElevation = MaterialTheme.dimensions.featureCardElevation,
        tonalElevation = MaterialTheme.dimensions.featureCardElevation,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column (
            Modifier
                .animateContentSize()   // has to be here to not mess up animation
                .height(
                    if (isExpanded) MaterialTheme.dimensions.featureCardExtendedHeight
                    else MaterialTheme.dimensions.featureCardHeight
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.dimensions.featureCardHeight)
            ) {
                contentComposable()
            }
            if (!isExpanded || extendedContentComposable == null) return@Column

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                extendedContentComposable()
            }
        }
    }
}