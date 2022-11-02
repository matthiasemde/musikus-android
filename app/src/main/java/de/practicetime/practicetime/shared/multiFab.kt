/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.shared

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

enum class MultiFABState {
    EXPANDED,
    COLLAPSED
}

data class MiniFABData(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun MultiFAB(
    state: MultiFABState,
    onStateChange: (MultiFABState) -> Unit,
    miniFABs: List<MiniFABData>
) {
    val transition = updateTransition(targetState = state, label = "transition")
    val rotate by transition.animateFloat(label = "rotate") {
        if (it == MultiFABState.EXPANDED) 135f else 0f
    }

    val miniFabScale by transition.animateFloat(label = "miniFabScale") {
        if (it == MultiFABState.EXPANDED) 1f else 0f
    }

    Column(modifier = Modifier
        .zIndex(1f),
        horizontalAlignment = Alignment.End
    ) {
        if (transition.targetState == MultiFABState.EXPANDED || transition.currentState == MultiFABState.EXPANDED) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .offset(y = (8 * (1 - miniFabScale)).dp)
                    .alpha(miniFabScale),
            ) {
                miniFABs.forEachIndexed { i, miniFAB ->
                    MiniFAB(
                        modifier = Modifier
                            .padding(8.dp)
                            .padding(bottom = (if (i+1 == miniFABs.size) 16 else 0).dp),
                        data = miniFAB
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { onStateChange(
                if(state == MultiFABState.EXPANDED) MultiFABState.COLLAPSED else MultiFABState.EXPANDED
            ) },
        ) {
            Icon(Icons.Default.Add, modifier = Modifier.rotate(rotate), contentDescription = "Expand")
        }
    }
}

@Composable
fun MiniFAB(
    modifier: Modifier,
    data: MiniFABData,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(end = 20.dp),
            text = data.label,
            style = MaterialTheme.typography.titleMedium
        )
        SmallFloatingActionButton(
            modifier = Modifier
                .size(40.dp),
            onClick = data.onClick,
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = data.label
            )
        }
    }
}