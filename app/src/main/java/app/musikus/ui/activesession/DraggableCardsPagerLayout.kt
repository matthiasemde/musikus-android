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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import app.musikus.ui.theme.dimensions
import app.musikus.ui.theme.spacing
import kotlinx.coroutines.launch


enum class DragValueY {
    Collapsed,
    Normal,
    Full,
}

data class DraggableCardPage(
    val content: @Composable () -> Unit,
    val header: @Composable () -> Unit = {},
    val title: String,
    val isExpandable: Boolean
)

val DRAG_HANDLE_HEIGHT = 3.dp

/**
 * Public interface for instantiating the complete pager.
 *
 * This has to be placed inside a Box()
 *
 * @param pageCount the number of pages
 * @param pages a function returning a DraggableCardPage for each page
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.DraggableCardsPagerLayout(
    pageCount: Int,
    anchorStates: List<AnchoredDraggableState<DragValueY>>,
    scrollStates: List<ScrollState>,
    pages: (Int) -> DraggableCardPage
) {

    Box(
        Modifier.align(Alignment.BottomCenter)
    ) {
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current

        val cardsPagerState = rememberPagerState(pageCount = { pageCount })
        val bottomPagerState = rememberPagerState(pageCount = { pageCount })

        val animationScope = rememberCoroutineScope()

        CardsPager(
            modifier = Modifier.zIndex(1f),
            pageContent = { pageIndex ->
                DraggableCard(
                    header = { pages(pageIndex).header() },
                    body = { pages(pageIndex).content() },
                    yState = anchorStates[pageIndex],
                    scrollState = scrollStates[pageIndex],
                    cardIsExpandable = pages(pageIndex).isExpandable,
                    onCollapsed = {
                        anchorStates.forEach {
                            animationScope.launch {
                                it.animateTo(DragValueY.Collapsed)
                            }
                        }
                    },
                    onUnCollapsed = {
                        anchorStates.forEach {
                            animationScope.launch {
                                it.animateTo(DragValueY.Normal)
                            }
                        }
                    }
                )
            },
            anchorStates = anchorStates,
            ownPagerState = cardsPagerState,
            bottomPagerState = bottomPagerState
        )

        BottomButtonPager(
            modifier = Modifier.align(Alignment.BottomCenter),
            pageTitles = { pages(it).title },
            ownPagerState = bottomPagerState,
            cardsPagerState = cardsPagerState,
        )

    }
}


/**
 * Returns a list of AnchoredDraggableStates for each page
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun getDraggableStateList(
    pageCount: Int
) : List<AnchoredDraggableState<DragValueY>> {
    val density = LocalDensity.current
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
                    density,
                    LocalConfiguration.current,
                    false
                ))
            }
        )
    }
    return stateList
}

/**
 * Returns a list of ScrollStates for each page.
 */
@Composable
fun getScrollableStateList(
    pageCount: Int
) : List<ScrollState> {
    val scrollStates = ArrayList<ScrollState>()
    repeat(pageCount) {
        scrollStates.add(rememberScrollState())
    }
    return scrollStates
}


/**
 * Returns the current fraction of the offset of the AnchoredDraggableState related to its normal position.
 *
 * @param state the AnchoredDraggableState
 * @return the fraction of the offset from normal position
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun getCurrentOffsetFraction(
    state: AnchoredDraggableState<DragValueY>
) : Float {

    val offsetPx = state.requireOffset()

    val maxOffset = if (offsetPx >= 0) {
        MaterialTheme.dimensions.cardNormalHeight - MaterialTheme.dimensions.cardPeekHeight
    } else {
        MaterialTheme.dimensions.cardNormalHeight -
                (CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN * LocalConfiguration.current.screenHeightDp).dp
    }
    val maxOffsetPx = with(LocalDensity.current) { maxOffset.toPx() }

    return (offsetPx / maxOffsetPx).coerceIn(0f, 1f)
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun getAnchors(
    density: Density,
    configuration: Configuration,
    expansionAllowed: Boolean = true
) : DraggableAnchors<DragValueY> {
    with(density) {

        val collapsedAllowed = true

        val heightExtended = (CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN * configuration.screenHeightDp).dp
        val heightNormal = MaterialTheme.dimensions.cardNormalHeight
        val heightPeek = MaterialTheme.dimensions.cardPeekHeight
        val travelOffset = (heightExtended - heightNormal)

        return DraggableAnchors {
            DragValueY.Normal at 0f
            DragValueY.Full at if (expansionAllowed) - travelOffset.toPx() else 0f
            DragValueY.Collapsed at if (collapsedAllowed) (heightNormal - heightPeek).toPx() else 0f
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
    pageContent : @Composable (page: Int) -> Unit,
    anchorStates : List<AnchoredDraggableState<DragValueY>>,
    ownPagerState: PagerState,
    bottomPagerState: PagerState
) {
    val maxContentPaddingDp = 16
    val state = anchorStates[ownPagerState.currentPage]


    val currentOffset = anchorStates[ownPagerState.currentPage].requireOffset()
    val fraction =
        if (currentOffset <= 0) getCurrentOffsetFraction(state = state)
        else 0f

    val pagerHeight = MaterialTheme.dimensions.bottomButtonsPagerHeight
    HorizontalPager(
        // bottom padding is dependent on the offsetFraction, means it will
        modifier = modifier
            .padding(bottom = ((1-fraction) * pagerHeight.value).dp),
        contentPadding = PaddingValues(
            start = ((1-fraction) * maxContentPaddingDp).dp,
            end = ((1-fraction) * maxContentPaddingDp).dp,
        ),
        pageSpacing = ((1-fraction) * maxContentPaddingDp).dp,
        state = ownPagerState,
        verticalAlignment = Alignment.Bottom,
        userScrollEnabled = currentOffset >= 0f // disallow scroll on fully opened cards
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
            .height(MaterialTheme.dimensions.bottomButtonsPagerHeight),
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
            )
            if (page == ownPagerState.currentPage) {
                Button(
                    modifier = Modifier.height(
                        // height is set height minus two times the "simulated" padding
                        // now make MaterialTheme.dimensions.draggableCardBottomButtonsHeight as high as asthetically pleasing
                        MaterialTheme.dimensions.bottomButtonsPagerHeight -
                        MaterialTheme.spacing.small - MaterialTheme.spacing.small
                    ),
                    contentPadding = contentPadding,
                    onClick = { onClick() },
                    content = { content() }
                )
            } else {
                TextButton(
                    modifier = Modifier.height(
                        MaterialTheme.dimensions.bottomButtonsPagerHeight -
                        MaterialTheme.spacing.small - MaterialTheme.spacing.small
                    ),
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
    cardIsExpandable: Boolean,
    onCollapsed: () -> Unit = {},    // callback when card is collapsed
    onUnCollapsed: () -> Unit = {}    // callback when card is uncollapsed
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val height = with(density) {
        MaterialTheme.dimensions.cardNormalHeight - yState.requireOffset().toDp()
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
            GrabHandle(dragState = yState)
            header()
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

    LaunchedEffect(key1 = yState.currentValue) {
        if (yState.currentValue == DragValueY.Collapsed) {
            onCollapsed()
        }
        if (yState.currentValue == DragValueY.Normal) {
            onUnCollapsed()
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GrabHandle(
    dragState: AnchoredDraggableState<DragValueY>
) {
    val animationScope = rememberCoroutineScope()
    Box (
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimensions.cardHandleHeight)
            .clickable {
                animationScope.launch {
                    if (dragState.currentValue == DragValueY.Collapsed) {
                        dragState.animateTo(DragValueY.Normal)
                    } else {
                        dragState.animateTo(DragValueY.Collapsed)
                    }
                }
            }
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