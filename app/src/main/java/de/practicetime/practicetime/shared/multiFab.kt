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

    Column(
        horizontalAlignment = Alignment.End
    ) {
        if (transition.targetState == MultiFABState.EXPANDED || transition.currentState == MultiFABState.EXPANDED) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .offset(y = (-8 * miniFabScale).dp)
                    .alpha(miniFabScale)
            ) {
                miniFABs.forEach { miniFAB ->
                    MiniFAB(data = miniFAB)
                }
                Spacer(modifier = Modifier.size(16.dp))
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
    data: MiniFABData,
) {
    Row (
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = data.label,
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(
            onClick = data.onClick,
            modifier = Modifier
                .size(48.dp)
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = data.label
            )
        }
    }
//    Canvas(
//        modifier = Modifier
//            .size(32.dp)
//            .clickable(
//                interactionSource = MutableInteractionSource(),
//                onClick = data.onClick,
//                indication = rememberRipple(
//                    bounded = false,
//                    radius = 20.dp,
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//            ),
//        ) {
//        drawCircle(
//            color = MaterialTheme.colorScheme.primaryContainer,
//            radius = 36f
//        )
//        Icon(data.icon, contentDescription = data.label)
//    }
}