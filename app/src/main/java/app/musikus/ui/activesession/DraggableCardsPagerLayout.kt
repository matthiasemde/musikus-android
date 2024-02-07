package app.musikus.ui.activesession

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 *
 */


/**
 * File encapsulating Logic For Bottom-Sheet like draggable Draggable Cards inside a HorizontalPager
 */

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.musikus.shared.conditional
import app.musikus.ui.theme.dimensions
import app.musikus.ui.theme.spacing
import kotlinx.coroutines.launch


enum class DragValueY {
    Collapsed,
    Normal,
    Full,
}


val DRAG_HANDLE_HEIGHT = 3.dp


interface DraggableCardHeaderUiState

interface DraggableCardBodyUiState

interface DraggableCardUiState <
    H : DraggableCardHeaderUiState,
    B : DraggableCardBodyUiState
>{
    val title : String
    val isExpandable: Boolean
    val hasFab: Boolean
    val headerUiState: H
    val bodyUiState: B
}

sealed class DraggableCardUiEvent {
    data object ExpandCard : DraggableCardUiEvent()
    data object CollapseCard : DraggableCardUiEvent()
    data object FabAction : DraggableCardUiEvent()
}


typealias DraggableCardUiEventHandler = (DraggableCardUiEvent) -> Unit

@OptIn(ExperimentalFoundationApi::class)
data class DraggableCardLocalState(
    val yState: AnchoredDraggableState<DragValueY>,
    val scrollState: ScrollState,
)

/**
 * Public interface for instantiating the complete pager.
 *
 * This has to be placed inside a Box()
 *
 * @param cardUiStates list of DraggableCardUiStates
 * @param cardHeaderComposable composable for the header
 * @param cardBodyComposable composable for the body
 * @param eventHandler event handler for the cards
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <
        C : DraggableCardUiState<H, B>,
        H : DraggableCardHeaderUiState,
        B : DraggableCardBodyUiState,
    > BoxScope.DraggableCardsPagerLayout(
    cardUiStates: List<C>,
    cardHeaderComposable: @Composable (H, DraggableCardLocalState, DraggableCardUiEventHandler) -> Unit,
    cardBodyComposable: @Composable (B, DraggableCardLocalState, DraggableCardUiEventHandler) -> Unit,
    eventHandler: (DraggableCardUiEvent) -> Unit
) {

    val pageCount = cardUiStates.size

    // DraggableAnchorState initialization. Each page gets own state.
    val anchorStates = rememberDraggableStates(pageCount = pageCount)
    val scrollStates = (0..pageCount).map { rememberScrollState() }

    val cardsPagerState = rememberPagerState(pageCount = { pageCount })
    val bottomPagerState = rememberPagerState(pageCount = { pageCount })

    val animationScope = rememberCoroutineScope()


    Box(
        Modifier.align(Alignment.BottomCenter)
    ) {
        CardsPager(
            modifier = Modifier.zIndex(1f),
            pageContent = { pageIndex ->
                val cardUiState = cardUiStates[pageIndex]
                val cardState = DraggableCardLocalState(
                    scrollState = scrollStates[pageIndex],
                    yState = anchorStates[pageIndex]
                )

                DraggableCard(
                    uiState = cardUiState,
                    headerComposable = cardHeaderComposable,
                    bodyComposable = cardBodyComposable,
                    cardState = cardState,
                    eventHandler = { event ->
                        when(event) {
                            is DraggableCardUiEvent.ExpandCard -> {
                                anchorStates.forEach {
                                    animationScope.launch {
                                        it.animateTo(DragValueY.Normal)
                                    }
                                }
                            }
                            is DraggableCardUiEvent.CollapseCard -> {
                                anchorStates.forEach {
                                    animationScope.launch {
                                        it.animateTo(DragValueY.Collapsed)
                                    }
                                }
                            }
                            else -> eventHandler(event)
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
            pageTitles = { pageIndex -> cardUiStates[pageIndex].title },
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
fun rememberDraggableStates(
    pageCount: Int
) : List<AnchoredDraggableState<DragValueY>> {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val extendedHeight = (CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN * configuration.screenHeightDp).dp
    val normalHeight = MaterialTheme.dimensions.cardNormalHeight
    val peekHeight = MaterialTheme.dimensions.cardPeekHeight

    return (0..< pageCount).map {
        remember {
            AnchoredDraggableState(
                anchors = getAnchors(
                    density = density,
                    extendedHeight = extendedHeight,
                    normalHeight = normalHeight,
                    peekHeight = peekHeight,
                    expansionAllowed = false
                ),
                initialValue = DragValueY.Normal,
                positionalThreshold = { distance: Float -> distance * 0.5f },
                velocityThreshold = { with(density) { 100.dp.toPx() } },
                animationSpec = tween()
            )
        }
    }
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

@OptIn(ExperimentalFoundationApi::class)
private fun getAnchors(
    density: Density,
    extendedHeight: Dp,
    normalHeight: Dp,
    peekHeight: Dp,
    expansionAllowed: Boolean = true
) : DraggableAnchors<DragValueY> {
    with(density) {

        val collapsedAllowed = true

        val travelOffset = (extendedHeight - normalHeight)

        return DraggableAnchors {
            DragValueY.Normal at 0f
            DragValueY.Full at if (expansionAllowed) - travelOffset.toPx() else 0f
            DragValueY.Collapsed at if (collapsedAllowed) (normalHeight - peekHeight).toPx() else 0f
        }
    }
}

/**
 * A HorizontalPager adjusting its contentPadding depending on the anchorState's distance to a
 * specific anchor.
 *
 * @param modifier Modifier
 * @param pageContent content of each page
 * @param anchorStates list of AnchoredDraggableStates for each page
 * @param ownPagerState PagerState for this pager
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
 * @param uiState the state of the card
 * @param headerComposable composable for the header
 * @param bodyComposable composable for the body
 * @param cardState the local state of the card
 * @param eventHandler event handler for the card
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <
        C : DraggableCardUiState<H, B>,
        H : DraggableCardHeaderUiState,
        B : DraggableCardBodyUiState
    > DraggableCard(
    uiState : C,
    headerComposable: @Composable (H, DraggableCardLocalState, DraggableCardUiEventHandler) -> Unit,
    bodyComposable: @Composable (B, DraggableCardLocalState, DraggableCardUiEventHandler) -> Unit,
    cardState: DraggableCardLocalState,
    eventHandler: DraggableCardUiEventHandler,
) {
    val yState = cardState.yState
    val scrollState = cardState.scrollState

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
        Surface (
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(
                topStart = MaterialTheme.shapes.large.topStart,
                topEnd = MaterialTheme.shapes.large.topEnd,
                bottomStart = MaterialTheme.shapes.large.bottomStart,
                bottomEnd = MaterialTheme.shapes.large.bottomEnd
            ),
            shadowElevation = 10.dp,
            tonalElevation = 10.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                GrabHandle(dragState = yState)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.dimensions.cardPeekContentHeight)
                ) {
                    headerComposable(
                        uiState.headerUiState,
                        cardState,
                        eventHandler
                    )
                }

                HorizontalDivider(Modifier.padding(horizontal = MaterialTheme.spacing.medium))

                Box(
                    modifier = Modifier
                        .conditional(
                            !uiState.isExpandable,
                            modifier = { requiredHeight(180.dp) }, // TODO fix with Michi
                            alternativeModifier = { verticalScroll(scrollState) }
                        )
                        .fillMaxWidth()
                ) {
                    bodyComposable(
                        uiState.bodyUiState,
                        cardState,
                        eventHandler
                    )
                }
            }
        }

        if (
            uiState.hasFab &&
            cardState.yState.currentValue != DragValueY.Collapsed
        ) {
            SmallFloatingActionButton(
                modifier = Modifier
                    .padding(MaterialTheme.spacing.medium)
                    .align(Alignment.BottomEnd),
                onClick = { eventHandler(DraggableCardUiEvent.FabAction) }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    }

    // update anchors according to required size inside
    // if content is scrollable (maxValue > 0), allow expanding
    // only update if in normal position to prevent force-shrinking it
    if (yState.requireOffset() == 0f) {

        val extendedHeight = (CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN * configuration.screenHeightDp).dp
        val normalHeight = MaterialTheme.dimensions.cardNormalHeight
        val peekHeight = MaterialTheme.dimensions.cardPeekHeight

        SideEffect {
            yState.updateAnchors(
                getAnchors(
                    density = density,
                    extendedHeight = extendedHeight,
                    normalHeight = normalHeight,
                    peekHeight = peekHeight,
                    expansionAllowed = uiState.isExpandable
                )
            )
        }
    }

    LaunchedEffect(key1 = yState.currentValue) {
        if (yState.currentValue == DragValueY.Collapsed) {
            eventHandler(DraggableCardUiEvent.CollapseCard)
        }
        if (yState.currentValue == DragValueY.Normal) {
            eventHandler(DraggableCardUiEvent.ExpandCard)
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