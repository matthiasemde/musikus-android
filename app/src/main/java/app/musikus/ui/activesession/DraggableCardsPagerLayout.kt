package app.musikus.ui.activesession

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */


/**
 * File encapsulating Logic For Bottom-Sheet like draggable Draggable Cards inside a HorizontalPager
 */

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.musikus.ui.theme.spacing


enum class DragValueY {
    Start,
    End
}

const val FRACTION_HEIGHT_COLLAPSED = 0.5f
const val FRACTION_HEIGHT_EXTENDED = 0.8f

/**
 * Public interface for instantiating the complete pager.
 *
 * This has to be placed inside a Box()
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableCardsPagerLayout(
    pageCount: Int,
    pageContent: @Composable (pageIndex: Int) -> Unit,
    headerContent: @Composable (pageIndex: Int) -> Unit,
    boxScope: BoxScope
) {
    with (boxScope) {
        Column(
            Modifier.align(Alignment.BottomCenter)
        ) {
            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            val maxOffsetPx: Float

            /** DraggableAnchorState initialization */
            val yAnchors = with(density) {
                val offset =
                    (FRACTION_HEIGHT_EXTENDED - FRACTION_HEIGHT_COLLAPSED) * configuration.screenHeightDp
                maxOffsetPx = -offset.dp.toPx()     //actual offset is negative (because swiping up), so invert
                DraggableAnchors {
                    DragValueY.Start at 0.dp.toPx()
                    DragValueY.End at -offset.dp.toPx()
                }
            }
            val yState = remember {
                AnchoredDraggableState(
                    initialValue = DragValueY.Start,
                    positionalThreshold = { distance: Float -> distance * 0.5f },
                    velocityThreshold = { with(density) { 100.dp.toPx() } },
                    animationSpec = tween()
                ).apply {
                    updateAnchors(yAnchors)
                }
            }

            CardsPager(
                pageContent = { pageIndex ->
                    DraggableCard(
                        header = { headerContent(pageIndex) },
                        body = { pageContent(pageIndex) },
                        yState = yState
                    )
                },
                anchorState = yState,
                pageCount = pageCount,
                maxOffsetPx = maxOffsetPx
            )
        }
    }
}


/**
 * A HorizontalPager adjusting its contentPadding depending on the anchorState's distance to a
 * specific anchor.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardsPager(
    pageContent : @Composable (page: Int) -> Unit,
    pageCount: Int,
    modifier: Modifier = Modifier,
    anchorState : AnchoredDraggableState<DragValueY>,
    maxOffsetPx: Float
) {
    Log.d("TAG", "maxOffsetPx}")

    val offsetFraction = (maxOffsetPx - anchorState.requireOffset()) / maxOffsetPx
    val maxContentPaddingDp = 30

    val pagerState = rememberPagerState(pageCount = { pageCount })
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        verticalAlignment = Alignment.Bottom,
        pageSpacing = MaterialTheme.spacing.medium,
        contentPadding = PaddingValues(
            start = (offsetFraction * maxContentPaddingDp).dp,
            end = (offsetFraction * maxContentPaddingDp).dp,
        ),
        userScrollEnabled = anchorState.requireOffset() == 0f
    ) {page ->
        pageContent(page)
    }
}



/**
 * DraggableCard BottomSheet-like implementation based on
 * https://www.droidcon.com/2021/11/09/how-to-master-swipeable-and-nestedscroll-modifiers-in-jetpack-compose/
 * and https://gist.github.com/arcadefire/7fe138c0ded1a36bee6dd57acdfa3d18,
 * adapted to use AnchoredDraggableState instead of SwipeableState
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraggableCard(
    yState : AnchoredDraggableState<DragValueY>,
    header: @Composable () -> Unit,
    body: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val scrollState = rememberScrollState()

    Box (
        Modifier
            .anchoredDraggable(yState, Orientation.Vertical)
            .height(with(density) {
                (configuration.screenHeightDp * FRACTION_HEIGHT_COLLAPSED).dp - yState
                    .requireOffset()
                    .toDp()
            })
//            .fillMaxWidth(0.8f + if (yState.requireOffset() < 0 ) { yState.progress*0.2f } else {0f})
            .nestedScroll(
                object : NestedScrollConnection {

                    /**
                     * Lets the parent (this Box) consume gestures before the inner list scrolls.
                     *
                     * Relevant when "opening" the Bottom Sheet so that it swipes up instead of scrolling.
                     */
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        val delta = available.y
                        return if (delta < 0) {
                            Offset(
                                x = 0f,
                                y = yState.dispatchRawDelta(delta)
                            )
                        } else {
                            Offset.Zero
                        }
                    }

                    /**
                     * Lets the parent (this Box) consume gestures after scrolling of the child is finished.
                     *
                     * Relevant for "closing" the Bottom Sheet so that it is dragged down when
                     * the list cannot be scrolled anymore.
                     */
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        if (source != NestedScrollSource.Drag) {
                            // We don't want to close the Bottom Sheet on "Fling" gestures,
                            // only when dragging
                            return Offset.Zero
                        }
                        val delta = available.y
                        return Offset(
                            x = 0f,
                            y = yState.dispatchRawDelta(delta)
                        )
                    }


                    /**
                     * Lets us actively invoke a "settle" (pull towards anchors) before a fling.
                     *
                     * Needed for getting pulled to the anchors on drag up when list is
                     * scrolled to the top.
                     */
                    override suspend fun onPreFling(available: Velocity): Velocity {
                        return if (available.y < 0 && scrollState.value == 0) {
                            yState.settle(velocity = available.y)
                            available
                        } else {
                            Velocity.Zero
                        }
                    }


                    /**
                     * Lets us actively invoke a "settle" (pull towards anchors) after a fling of the
                     * scrollable child.
                     *
                     * Needed for closing the Bottom sheet completely when starting to "drag it down".
                     */
                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity
                    ): Velocity {
                        yState.settle(velocity = available.y)
                        return super.onPostFling(consumed, available)
                    }
                }
            )
    ){
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp))
                .fillMaxHeight()
        ) {
            header()
            Box(
                Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
            ) {
                body()
            }
        }
    }
}