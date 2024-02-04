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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.musikus.ui.theme.spacing
import kotlinx.coroutines.launch


enum class DragValueY {
    Collapsed,
    Normal,
    Full,
}

data class DraggableCardPage(
    val content: @Composable () -> Unit,
    val title: String,
    val isExpandable: Boolean
)

/**
 * Public interface for instantiating the complete pager.
 *
 * This has to be placed inside a Box()
 *
 * @param pageCount the number of pages
 * @param pageContent content of each page (composable)
 * @param headerContent header content of each page (composable)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.DraggableCardsPagerLayout(
    pageCount: Int,
    pages: (Int) -> DraggableCardPage
) {
    Box(
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

        val cardsPagerState = rememberPagerState(pageCount = { pageCount })
        val bottomPagerState = rememberPagerState(pageCount = { pageCount })

        CardsPager(
            modifier = Modifier.zIndex(1f),
            pageContent = { pageIndex ->
                DraggableCard(
                    header = { CardHeader(text = pages(pageIndex).title) },
                    body = { pages(pageIndex).content() },
                    yState = anchorStates[pageIndex],
                    scrollState = scrollStates[pageIndex],
                    cardIsExpandable = pages(pageIndex).isExpandable
                )
            },
            anchorStates = anchorStates,
            pageCount = pageCount,
            maxOffsetPx = with(density) {
                val heightExtended =
                    (CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN * configuration.screenHeightDp).dp
                val heightCollapsed = CARD_HEIGHT_NORMAL
                -(heightExtended - heightCollapsed).toPx()// negative value because sliding up
            },
            ownPagerState = cardsPagerState,
            bottomPagerState = bottomPagerState
        )

        BottomButtonPager(
            modifier = Modifier.align(Alignment.BottomCenter),
            pageCount = pageCount,
            pageTitles = { pages(it).title },
            ownPagerState = bottomPagerState,
            cardsPagerState = cardsPagerState,
        )

    }
}

@Composable
private fun CardHeader(
    text: String
) {
    Column (
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(Modifier.height(MaterialTheme.spacing.large))
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        HorizontalDivider()
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun getAnchors(
    density: Density,
    configuration: Configuration,
    expansionAllowed: Boolean = true
) : DraggableAnchors<DragValueY> {
    with(density) {

        val collapsedAllowed = true

        val heightExtended = (CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN * configuration.screenHeightDp).dp
        val heightCollapsed = CARD_HEIGHT_NORMAL
        val travelOffset = (heightExtended - heightCollapsed)

        return DraggableAnchors {
            DragValueY.Normal at 0f
            DragValueY.Full at if (expansionAllowed) -travelOffset.toPx() else 0f
            DragValueY.Collapsed at if (collapsedAllowed) (CARD_HEIGHT_NORMAL - CARD_HEIGHT_PEEK).toPx() else 0f
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
    ownPagerState: PagerState,
    bottomPagerState: PagerState
) {
    val currentOffset = anchorStates[ownPagerState.currentPage].requireOffset()
    val offsetFraction = minOf((maxOffsetPx - currentOffset) / maxOffsetPx, 1f)
        .coerceAtLeast(0f)  // prevent negative values (once experienced due to floating point errors I guess)
    val maxContentPaddingDp = 16

    HorizontalPager(
        modifier = modifier.padding(bottom = (offsetFraction * HEIGHT_BOTTOM_BUTTONS_DP).dp),
        state = ownPagerState,
        verticalAlignment = Alignment.Bottom,
        pageSpacing = MaterialTheme.spacing.medium,
        contentPadding = PaddingValues(
            start = (offsetFraction * maxContentPaddingDp).dp,
            end = (offsetFraction * maxContentPaddingDp).dp,
        ),
        userScrollEnabled = anchorStates[ownPagerState.currentPage].requireOffset() >= 0f
    ) {page ->
        pageContent(page)
    }
    // sync bottomPagerState with ownPagerState on scroll
    LaunchedEffect(key1 = ownPagerState.currentPage) {
        bottomPagerState.animateScrollToPage(ownPagerState.currentPage)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomButtonPager(
    modifier: Modifier = Modifier,
    pageCount: Int,
    pageTitles: (Int) -> String,
    ownPagerState: PagerState,
    cardsPagerState: PagerState
) {

    val effectivePageWidth = 110.dp  // the amount of dp one button has when centered
    val screenWidth = LocalConfiguration.current.run {
        screenWidthDp.dp
    }
    val animationScope = rememberCoroutineScope()
    val manuallyClicked = remember{ mutableStateOf(false) }

    HorizontalPager(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(top = 20.dp)
            .height(HEIGHT_BOTTOM_BUTTONS_DP.dp),
        state = ownPagerState,
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(
            start = screenWidth/2 - effectivePageWidth/2,
            end = screenWidth/2 - effectivePageWidth/2
        )
    ) {page ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            fun onClick() {
                // based on https://github.com/androidx/androidx/blob/ef05d08d7cc4ae20ada9dd176f
                // c2fc52c574c5c6/compose/foundation/foundation/samples/src/main/java/androidx/
                // compose/foundation/samples/PagerSamples.kt#L266
                animationScope.launch {
                    manuallyClicked.value = true
                    ownPagerState.animateScrollToPage(page)
                    manuallyClicked.value = false
                }
                animationScope.launch {
                    manuallyClicked.value = true
                    cardsPagerState.animateScrollToPage(page)
                    manuallyClicked.value = false
                }
            }

            @Composable
            fun content() {
                Text(
                    text = pageTitles(page),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }

            val contentPadding = PaddingValues(
                start =  MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                top = MaterialTheme.spacing.small,
                bottom = MaterialTheme.spacing.small,
            )
            if (page == ownPagerState.currentPage) {
                Button(
                    contentPadding = contentPadding,
                    onClick = { onClick() },
                    content = { content() }
                )
            } else {
                TextButton(
                    contentPadding = contentPadding,
                    onClick = { onClick() },
                    content = { content() }
                )
            }
        }
    }
    // prevent the call on button click because jumping over a page wouldn't work anymore since
    // this call will lead to the other pager again changing this pager hindering us
    if (!manuallyClicked.value) {
        // sync bottomPagerState with ownPagerState on scroll
        LaunchedEffect(key1 = ownPagerState.currentPage) {
            cardsPagerState.animateScrollToPage(ownPagerState.currentPage)
        }
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
    scrollState: ScrollState,
    cardIsExpandable: Boolean
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val height = with(density) {
        CARD_HEIGHT_NORMAL - yState.requireOffset().toDp()
    }

//    val cardIsExpandable = scrollState.maxValue > 0

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
        ElevatedCard (
            modifier = Modifier.fillMaxHeight(),
            shape = RoundedCornerShape(
                topStart = MaterialTheme.shapes.large.topStart,
                topEnd = MaterialTheme.shapes.large.topEnd,
                bottomStart = MaterialTheme.shapes.large.bottomStart,
                bottomEnd = MaterialTheme.shapes.large.bottomEnd
            ),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            GrabHandle()
//            header()
            Box(
                Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.small),

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
                density, configuration, cardIsExpandable
            )
        )
    }
}

@Composable
private fun GrabHandle() {
    Row (
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Surface (
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .padding(MaterialTheme.spacing.small)
                .size(
                    width = 25.dp,
                    height = 3.dp
                ),
            shape = RoundedCornerShape(50),
            content = { }
        )
    }
}