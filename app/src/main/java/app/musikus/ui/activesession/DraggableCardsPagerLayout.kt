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

import android.content.res.Configuration
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.musikus.ui.theme.spacing


enum class DragValueY {
    Collapsed,
    Normal,
    Full,
}

const val FRACTION_HEIGHT_COLLAPSED = 0.5f
const val FRACTION_HEIGHT_EXTENDED = 0.8f

/**
 * Public interface for instantiating the complete pager.
 *
 * This has to be placed inside a Box()
 *
 * @param pageCount the number of pages
 * @param pageContent content of each page (composable)
 * @param headerContent header content of each page (composable)
 * @param boxScope the outer BoxScope
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

            /** scroll states for each page */
            val scrollStates = ArrayList<ScrollState>()
            for (i in 0..< pageCount) {
                val scrollState = rememberScrollState()
                scrollStates.add(scrollState)
            }

            /** DraggableAnchorState initialization. Each page gets own state. */
            val stateList = ArrayList<AnchoredDraggableState<DragValueY>>()
            for (i in 0..< pageCount) {
                stateList.add(
                    AnchoredDraggableState(
                        initialValue = DragValueY.Normal,
                        positionalThreshold = { distance: Float -> distance * 0.5f },
                        velocityThreshold = { with(density) { 100.dp.toPx() } },
                        animationSpec = tween()
                    ).apply {
                        updateAnchors(getAnchors(
                            LocalDensity.current,
                            LocalConfiguration.current,
                            false
                        ))
                    }
                )
            }
            val anchorStates = remember { stateList }

            CardsPager(
                pageContent = { pageIndex ->
                    DraggableCard(
                        header = { headerContent(pageIndex) },
                        body = { pageContent(pageIndex) },
                        yState = anchorStates[pageIndex],
                        scrollState = scrollStates[pageIndex]
                    )
                },
                anchorStates = anchorStates,
                pageCount = pageCount,
                maxOffsetPx = with(density) {
                        -(FRACTION_HEIGHT_EXTENDED - FRACTION_HEIGHT_COLLAPSED) *
                                configuration.screenHeightDp.dp.toPx()
                }
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
private fun getAnchors(
    density: Density,
    configuration: Configuration,
    expansionAllowed: Boolean = true
) : DraggableAnchors<DragValueY> {
    with(density) {

        val collapsedAllowed = false
        val offset =
            (FRACTION_HEIGHT_EXTENDED - FRACTION_HEIGHT_COLLAPSED) * configuration.screenHeightDp

        return DraggableAnchors {
            DragValueY.Normal at 0f
            DragValueY.Full at if (expansionAllowed) { -offset.dp.toPx() } else { 0f }
            DragValueY.Collapsed at if (collapsedAllowed) 200.dp.toPx() else 0f
        }
    }
}

/**
 * A HorizontalPager adjusting its contentPadding depending on the anchorState's distance to a
 * specific anchor.
 *
 * @param pageCount number of pages
 * @param pageContent content of each page
 * @param anchorStates list of AnchoredDraggableStates for each page
 * @param maxOffsetPx maximum offset in pixels (usually negative). Used to calculate the padding
 * @param modifier Modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardsPager(
    modifier: Modifier = Modifier,
    pageCount: Int,
    pageContent : @Composable (page: Int) -> Unit,
    anchorStates : List<AnchoredDraggableState<DragValueY>>,
    maxOffsetPx: Float,
) {
    val pagerState = rememberPagerState(pageCount = { pageCount })

    val offsetFraction = (maxOffsetPx - anchorStates[pagerState.currentPage].requireOffset()) / maxOffsetPx
    val maxContentPaddingDp = 30

    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        verticalAlignment = Alignment.Bottom,
        pageSpacing = MaterialTheme.spacing.medium,
        contentPadding = PaddingValues(
            start = (offsetFraction * maxContentPaddingDp).dp,
            end = (offsetFraction * maxContentPaddingDp).dp,
        ),
        userScrollEnabled = anchorStates[pagerState.currentPage].requireOffset() >= 0f
    ) {page ->
        pageContent(page)
    }
}



/**
 * DraggableCard BottomSheet-like implementation based on
 * https://www.droidcon.com/2021/11/09/how-to-master-swipeable-and-nestedscroll-modifiers-in-jetpack-compose/
 * and https://gist.github.com/arcadefire/7fe138c0ded1a36bee6dd57acdfa3d18,
 * adapted to use AnchoredDraggableState instead of SwipeableState
 *
 * @param yState AnchoredDraggableState for the vertical axis
 * @param header header content composable
 * @param body body content composable
 * @param scrollState ScrollState for the body content
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraggableCard(
    yState : AnchoredDraggableState<DragValueY>,
    header: @Composable () -> Unit,
    body: @Composable () -> Unit,
    scrollState: ScrollState
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val height = with(density) {
        (configuration.screenHeightDp * FRACTION_HEIGHT_COLLAPSED).dp - yState.requireOffset().toDp()
    }

    Box (
        Modifier
            .anchoredDraggable(yState, Orientation.Vertical)
            .height(height)
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

    // update anchors according to required size inside
    // if content is scrollable (maxValue > 0), allow expanding
    // only update if in normal position to prevent force-shrinking it
    if (yState.requireOffset() == 0f) {
        yState.updateAnchors(
            getAnchors(
                density,configuration,scrollState.maxValue > 0
            )
        )
    }
}