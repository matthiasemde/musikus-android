/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.ui.library

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.composethemeadapter3.Mdc3Theme
import com.google.android.material.composethemeadapter3.Theme3Parameters
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.Category

enum class LibrarySortMode {
    DATE_ADDED,
    LAST_MODIFIED,
    NAME,
    COLOR,
    CUSTOM
}

enum class LibraryMenuSelections {
    SORT_BY,
}

enum class CommonMenuSelections {
    THEME,
    APP_INFO
}

enum class ThemeSubMenuSelections {
    DAY,
    NIGHT,
}

class LibraryState() {
    var showMainMenu = mutableStateOf(false)
    var showThemeSubMenu = mutableStateOf(false)
    var showSortModeSubMenu = mutableStateOf(false)

    var items = mutableStateListOf<Category>()

    var sortMode = mutableStateOf(LibrarySortMode.DATE_ADDED)
    var sortDirection = mutableStateOf(true)

    fun sortItems(mode: LibrarySortMode) {
        if(mode == sortMode.value) {
            sortDirection.value = !sortDirection.value
        } else {
            sortDirection.value = true
            sortMode.value = mode
            when (sortMode.value) {
                LibrarySortMode.DATE_ADDED -> items.sortBy { it.createdAt }
                LibrarySortMode.NAME -> items.sortBy { it.name }
                LibrarySortMode.COLOR -> items.sortBy { it.colorIndex }
                LibrarySortMode.LAST_MODIFIED -> items.sortBy { it.modifiedAt }
                LibrarySortMode.CUSTOM -> items.sortBy { it.createdAt } // TODO: Not implemented yet
            }
        }
        if(sortDirection.value) {
            items.reverse()
        }

        PracticeTime.prefs.edit().putString(
            PracticeTime.PREFERENCES_KEY_LIBRARY_SORT_MODE,
            sortMode.toString()
        ).apply()
    }
}

@Composable
fun rememberLibraryState() = remember() { LibraryState() }

class LibraryFragment : Fragment() {

    private var categoryAdapter : CategoryAdapter? = null

    private var addCategoryDialog: CategoryDialog? = null
    private var editCategoryDialog: CategoryDialog? = null
    private var deleteCategoryDialog: AlertDialog? = null

    private lateinit var libraryToolbar: MaterialToolbar
    private lateinit var libraryCollapsingToolbarLayout: CollapsingToolbarLayout

    private var actionMode: ActionMode? = null

    private val selectedItems = ArrayList<Long>()

    // catch the back press for the case where the selection should be reverted
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(selectedItems.isNotEmpty()){
                    actionMode?.finish()
                }else{
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        })
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView (
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        libraryToolbar = view.findViewById(R.id.libraryToolbar)
//        libraryCollapsingToolbarLayout = view.findViewById(R.id.libraryToolbarLayout)
//        initToolbar()  // initialize the toolbar with all its listeners


        return ComposeView(requireContext()).apply {
            setContent {
                val libraryState = rememberLibraryState()
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

                val sortMode = try {
                    LibrarySortMode.valueOf(
                        PracticeTime.prefs.getString(
                            PracticeTime.PREFERENCES_KEY_LIBRARY_SORT_MODE,
                            LibrarySortMode.DATE_ADDED.toString()
                        ) ?: LibrarySortMode.DATE_ADDED.toString()
                    )
                } catch (ex: Exception) {
                    LibrarySortMode.DATE_ADDED
                }

                LaunchedEffect(true) {
                    PracticeTime.categoryDao.get(activeOnly = true).let {
                        // sort categories depending on the current sort mode
                        libraryState.items.addAll(it)
                        libraryState.sortItems(sortMode)
                    }
                }

                Mdc3Theme {
                    Scaffold(
                        modifier = Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            MediumTopAppBar(
                                scrollBehavior = scrollBehavior,
                                title = {
                                    Text(text = "Library")
                                },
                                actions = {
                                    IconButton(onClick = { libraryState.showMainMenu.value = !libraryState.showMainMenu.value }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                                    }
                                    MainMenu(
                                        show = libraryState.showMainMenu.value,
                                        onDismissHandler = { libraryState.showMainMenu.value = false },
                                        onSelectionHandler = { librarySelection, commonSelection ->
                                            libraryState.showMainMenu.value = false
                                            when(librarySelection) {
                                                LibraryMenuSelections.SORT_BY -> {
                                                    libraryState.showSortModeSubMenu.value = true
                                                }
                                                null -> {}
                                            }
                                            when(commonSelection) {
                                                CommonMenuSelections.APP_INFO -> {}
                                                CommonMenuSelections.THEME -> {
                                                    libraryState.showThemeSubMenu.value = true
                                                }
                                                null -> {}
                                            }
                                        }
                                    )
                                    LibrarySubMenuSortMode(
                                        show = libraryState.showSortModeSubMenu.value,
                                        onDismissHandler = { libraryState.showSortModeSubMenu.value = false },
                                        sortMode = libraryState.sortMode.value,
                                        sortDirection = libraryState.sortDirection.value,
                                        onSelectionHandler = { sortMode ->
                                            libraryState.showSortModeSubMenu.value = false
                                            libraryState.sortItems(sortMode)
                                        }
                                    )
                                }
                            )
                        },
                        content = { contentPadding ->
                            LibraryItemListComposable(
                                items = libraryState.items,
                                selectedItems = selectedItems,
                                contentPadding = contentPadding,
                            )
                        }
                    )
                }
            }
        }
//        sortMode = try {
//            LibrarySortMode.valueOf(
//                PracticeTime.prefs.getString(
//                    PracticeTime.PREFERENCES_KEY_LIBRARY_SORT_MODE,
//                    LibrarySortMode.DATE_ADDED.toString()
//                ) ?: LibrarySortMode.DATE_ADDED.toString()
//            )
//        } catch (ex: Exception) {
//            LibrarySortMode.DATE_ADDED
//        }
//
////        initCategoryList()
//
//        // create a new category dialog for adding new categories
//        addCategoryDialog = CategoryDialog(requireActivity(), ::addCategoryHandler)
//
//        view.findViewById<FloatingActionButton>(R.id.libraryFab).setOnClickListener {
//            actionMode?.finish()
//            addCategoryDialog?.show()
//        }
//
//        // create the category dialog for editing categories
//        editCategoryDialog = CategoryDialog(requireActivity(), ::editCategoryHandler)
//
//        // create the dialog for archiving categories
//        initDeleteCategoryDialog()
    }

    @Composable
    fun CommonMenuItems(
        onSelectionHandler: (CommonMenuSelections) -> Unit
    ) {
        DropdownMenuItem(
            text = { Text(text = "Theme") },
            trailingIcon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
            onClick = { onSelectionHandler(CommonMenuSelections.THEME) }
        )
        DropdownMenuItem(
            modifier = Modifier.padding(end = 50.dp),
            text = { Text(text = "App Info") },
            onClick = { onSelectionHandler(CommonMenuSelections.APP_INFO) }
        )
    }

    @Composable
    fun LibrarySubMenuSortMode(
        show: Boolean,
        onDismissHandler: () -> Unit,
        sortMode: LibrarySortMode,
        sortDirection: Boolean,
        onSelectionHandler: (LibrarySortMode) -> Unit
    ) {
        DropdownMenu(
            expanded = show,
            onDismissRequest = onDismissHandler,
        ) {
            val directionIcon: @Composable () -> Unit = {
                if (sortDirection)
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                else
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            val primaryColor = Color(PracticeTime.getThemeColor(R.attr.colorPrimary, requireContext()))
            DropdownMenuItem(
                text = { Text(
                    text = "Date Added",
                    color = if (sortMode == LibrarySortMode.DATE_ADDED) primaryColor else Color.Unspecified
                ) },
                onClick = { onSelectionHandler(LibrarySortMode.DATE_ADDED) },
                trailingIcon = if (sortMode == LibrarySortMode.DATE_ADDED) directionIcon else null
            )
            DropdownMenuItem(
                text = { Text(
                    text = "Last modified",
                    color = if (sortMode == LibrarySortMode.LAST_MODIFIED) primaryColor else Color.Unspecified
                ) },
                onClick = { onSelectionHandler(LibrarySortMode.LAST_MODIFIED) },
                trailingIcon = if (sortMode == LibrarySortMode.LAST_MODIFIED) directionIcon else null
            )
            DropdownMenuItem(
                text = { Text(
                    text = "Name",
                    color = (if (sortMode == LibrarySortMode.NAME) primaryColor else Color.Unspecified)
                ) },
                onClick = { onSelectionHandler(LibrarySortMode.NAME) },
                trailingIcon = if (sortMode == LibrarySortMode.NAME) directionIcon else null
            )
            DropdownMenuItem(
                modifier = Modifier.padding(end = 50.dp),
                text = { Text(
                    text = "Color",
                    color = if (sortMode == LibrarySortMode.COLOR) primaryColor else Color.Unspecified
                ) },
                onClick = { onSelectionHandler(LibrarySortMode.COLOR) },
                trailingIcon = if (sortMode == LibrarySortMode.COLOR) directionIcon else null
            )
        }
    }

    @Composable
    fun LibraryMenuItems(
        onSelectionHandler: (LibraryMenuSelections) -> Unit
    ) {
        DropdownMenuItem(
            text = { Text(text = "Sort by") },
            onClick = { onSelectionHandler(LibraryMenuSelections.SORT_BY) },
            trailingIcon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) }
        )
    }

    @Composable
    fun MainMenu(
        show: Boolean,
        onDismissHandler: () -> Unit,
        onSelectionHandler: (
            librarySelection: LibraryMenuSelections?,
            commonSelection: CommonMenuSelections?
        ) -> Unit
    ) {
        DropdownMenu(
            expanded = show,
            onDismissRequest = onDismissHandler,
        ) {
            LibraryMenuItems(
                onSelectionHandler = { onSelectionHandler(it, null) }
            )
            CommonMenuItems(
                onSelectionHandler = { onSelectionHandler( null, it) }
            )
        }
    }

    @Composable
    fun LibraryItemListComposable(
        items: List<Category>,
        selectedItems: List<Long>,
        contentPadding: PaddingValues
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(top = 12.dp)
                .padding(horizontal = 8.dp),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                LibraryItemComposable(
                    category = item,
                    selected = selectedItems.contains(item.id)
                )
            }
        }
    }

    @Composable
    fun LibraryItemComposable(
        category: Category,
        selected: Boolean
//        itemClickedCallback: (callbackListDataItem: CallbackListDataItem) -> Unit,
    ) {
        AndroidView(
            factory = { context ->
                val view = LayoutInflater.from(context).inflate(R.layout.listitem_library_item, null)

                val cardView = view.findViewById<CardView>(R.id.library_item_card)
                val colorIndicatorView = view.findViewById<ImageView>(R.id.library_item_color_indicator)
                val colorOverlayView = view.findViewById<ImageView>(R.id.library_item_color_overlay)
                val nameView = view.findViewById<TextView>(R.id.library_item_name)

                cardView.isSelected = selected
                colorOverlayView.backgroundTintList = ColorStateList.valueOf(
                    PracticeTime.getCategoryColors(requireContext())[category.colorIndex]
                )
                colorIndicatorView.backgroundTintList = ColorStateList.valueOf(
                    PracticeTime.getCategoryColors(requireContext())[category.colorIndex]
                )

                nameView.text = category.name

                view // return the view
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            update = { view ->
                // Update the view
            }
        )
    }


//    private fun initCategoryList() {
//        categoryAdapter = CategoryAdapter(
//            libraryItems,
//            selectedItems,
//            context = requireActivity(),
//            shortClickHandler = ::shortClickOnCategoryHandler,
//            longClickHandler = ::longClickOnCategoryHandler,
//        )
//
//        // load all active categories from the database and notify the adapter
//        lifecycleScope.launch {
//            PracticeTime.categoryDao.get(activeOnly = true).let {
//                // sort categories depending on the current sort mode
//                libraryItems.addAll(it)
//                Log.d("LIBRARY","$sortMode")
//                sortCategoryList(sortMode)
////                categoryAdapter?.notifyItemRangeInserted(0, it.size) // sortCategoryList() already notifies the adapter
//            }
//
////            requireActivity().findViewById<RecyclerView>(R.id.libraryCategoryList).apply {
//////                layoutManager = GridLayoutManager(context, 2)
////                layoutManager = LinearLayoutManager(context)
////                adapter = categoryAdapter
////                itemAnimator?.apply {
////                    addDuration = 500L
////                    moveDuration = 500L
////                    removeDuration = 200L
////                }
////            }
//
//            if (libraryItems.isEmpty()) showHint()
//        }
//    }

    // initialize the category delete dialog
    private fun initDeleteCategoryDialog() {
        deleteCategoryDialog = AlertDialog.Builder(requireActivity()).apply {
            setPositiveButton(R.string.deleteDialogConfirm) { dialog, _ ->
//                deleteCategoriesHandler()
                dialog.dismiss()
            }
            setNegativeButton(R.string.dialogCancel) { dialog, _ ->
                dialog.cancel()
            }
        }.create()
    }

    private fun actionModeCallback(): ActionMode.Callback {
        return object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.library_toolbar_menu_for_selection, menu)
                libraryCollapsingToolbarLayout.apply {
                    setBackgroundColor(PracticeTime.getThemeColor(R.attr.colorSurface, requireContext()))
                    contentScrim = null
                }
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return when(item?.itemId) {
                    R.id.topToolbarSelectionEdit -> {
                        // editCategoryDialog?.show(libraryItems[selectedItems.first().toInt()])
                        true
                    }
                    R.id.topToolbarSelectionDelete -> {
                        if(PracticeTime.serviceIsRunning)
                            Toast.makeText(context, getString(R.string.cannot_delete_error), Toast.LENGTH_SHORT).show()
                        else {
                            deleteCategoryDialog?.apply {
                                setMessage(
                                    context.getString(
                                        if (selectedItems.size > 1) R.string.deleteCategoriesDialogMessage
                                        else R.string.deleteCategoryDialogMessage
                                    )
                                )
                                show()
                            }
                        }
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                clearCategorySelection()
                val transparentSurfaceColor =
                    ColorUtils.setAlphaComponent(PracticeTime.getThemeColor(R.attr.colorSurface, requireContext()), 0)
                libraryCollapsingToolbarLayout.apply {
                    val backgroundColorAnimation = ValueAnimator.ofObject(
                        ArgbEvaluator(),
                        PracticeTime.getThemeColor(R.attr.colorSurface, requireContext()),
                        transparentSurfaceColor
                    )
                    backgroundColorAnimation.duration = 500 // milliseconds

                    backgroundColorAnimation.addUpdateListener { animator ->
                        setBackgroundColor(animator.animatedValue as Int)
                    }
                    backgroundColorAnimation.start()

                    val scrimColorAnimation = ValueAnimator.ofObject(
                        ArgbEvaluator(),
                        PracticeTime.getThemeColor(R.attr.colorSurface, requireContext()),
                        transparentSurfaceColor
                    )
                    scrimColorAnimation.duration = 340 // milliseconds

                    scrimColorAnimation.addUpdateListener { animator ->
                        setStatusBarScrimColor(animator.animatedValue as Int)
                    }
                    scrimColorAnimation.start()
                    setContentScrimColor(PracticeTime.getThemeColor(R.attr.backgroundToolbarCollapsed, requireContext()))
                }
            }
        }
    }

    private fun shortClickOnCategoryHandler(index: Int) {
        // if there are already categories selected,
        // add or remove the clicked category from the selection
//        if(selectedItems.isNotEmpty()) {
//            if(selectedItems.removeAt(index)) {
//                categoryAdapter?.notifyItemChanged(index)
//                if(selectedItems.size == 1) {
//                    actionMode?.menu?.findItem(R.id.topToolbarSelectionEdit)?.isVisible = true
//                } else if(selectedItems.isEmpty()) {
//                    actionMode?.finish()
//                }
//            } else {
//                longClickOnCategoryHandler(index, vibrate = false)
//            }
//            actionMode?.title = "${selectedItems.size} selected"
//        } else {
//            editCategoryDialog?.show(libraryItems[index])
//        }
    }

    // the handler for dealing with long clicks on category
    private fun longClickOnCategoryHandler(index: Int, vibrate: Boolean = true): Boolean {
        // if there is no category selected already, change the toolbar
        if(selectedItems.isEmpty()) {
            actionMode = libraryToolbar.startActionMode(actionModeCallback())
            actionMode?.title = "1 selected"
        }

        if(!selectedItems.contains(index.toLong())) {
            if (vibrate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (requireContext().getSystemService(
                    Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                        ).defaultVibrator.apply {
                        cancel()
                        vibrate(
                            VibrationEffect.createOneShot(100,100)
                        )
                    }
            }
            // now add the newly selected category to the list...
            selectedItems.add(index.toLong())
            categoryAdapter?.notifyItemChanged(index)
        }

        actionMode?.menu?.findItem(R.id.topToolbarSelectionEdit)?.isVisible =
            selectedItems.size == 1

        // we consumed the event so we return true
        return true
    }

    private fun clearCategorySelection() {
        val tmpCopy = selectedItems.toList()
        selectedItems.clear()
        tmpCopy.forEach { categoryAdapter?.notifyItemChanged(it.toInt()) }
    }

    // init the toolbar and associated data
//    private fun initToolbar() {
//        libraryToolbar.apply {
//            menu?.clear()
//            setCommonToolbar(requireActivity(), this) {
//                when(it) {
//                    R.id.libraryToolbarSortModeDateAdded -> sortCategoryList(LibrarySortMode.DATE_ADDED)
//                    R.id.libraryToolbarSortModeLastModified -> sortCategoryList(LibrarySortMode.LAST_MODIFIED)
//                    R.id.libraryToolbarSortModeName -> sortCategoryList(LibrarySortMode.NAME)
//                    R.id.libraryToolbarSortModeColor -> sortCategoryList(LibrarySortMode.COLOR)
//                    R.id.libraryToolbarSortModeCustom -> sortCategoryList(LibrarySortMode.CUSTOM)
//                    else -> {}
//                }
//            }
//            inflateMenu(R.menu.library_toolbar_menu_base)
////            setNavigationIcon(R.drawable.ic_account)
//        }
//    }

//    // the handler for creating new categories
//    private fun addCategoryHandler(newCategory: Category) {
//        lifecycleScope.launch {
//            PracticeTime.categoryDao.insertAndGet(newCategory)
//                ?.let { libraryItems.add(0, it) }
//            categoryAdapter?.notifyItemInserted(0)
//            if(libraryItems.isNotEmpty()) hideHint()
//        }
//    }
//
//    // the handler for editing categories
//    private fun editCategoryHandler(category: Category) {
//        lifecycleScope.launch {
//            PracticeTime.categoryDao.update(category)
//            libraryItems.indexOfFirst { c -> c.id == category.id }.also { i ->
//                assert(i != -1) {
//                    Log.e("EDIT_CATEGORY", "No category with matching id found for\n$category")
//                }
//                libraryItems[i] = category
//                categoryAdapter?.notifyItemChanged(i)
//            }
//            actionMode?.finish()
//        }
//    }
//
//    // the handler for deleting categories
//    private fun deleteCategoriesHandler() {
//        var failedDeleteFlag = false
//        lifecycleScope.launch {
//            selectedItems.sortedByDescending { it }.forEach { index ->
//                if(PracticeTime.categoryDao.archive(libraryItems[index.toInt()].id)) {
//                    libraryItems.removeAt(index.toInt())
//                    categoryAdapter?.notifyItemRemoved(index.toInt())
//                } else {
//                    failedDeleteFlag = true
//                }
//            }
//
//            if(failedDeleteFlag) {
//                Snackbar.make(
//                    requireView(),
//                    if(selectedItems.size > 1) R.string.deleteCategoriesFailSnackbar
//                    else R.string.deleteCategoryFailSnackbar,
//                    5000
//                ).show()
//            } else {
//                Toast.makeText(
//                    requireActivity(),
//                    if(selectedItems.size > 1) R.string.deleteCategoriesSuccessSnackbar
//                    else R.string.deleteCategorySuccessSnackbar,
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//
//            if(libraryItems.isEmpty()) showHint()
//            actionMode?.finish()
//        }
//    }

    private fun showHint() {
        requireView().apply {
            findViewById<TextView>(R.id.libraryHint).visibility = View.VISIBLE
//            findViewById<RecyclerView>(R.id.libraryCategoryList).visibility = View.GONE
        }
    }

    private fun hideHint() {
        requireView().apply {
            findViewById<TextView>(R.id.libraryHint).visibility = View.GONE
//            findViewById<RecyclerView>(R.id.libraryCategoryList).visibility = View.VISIBLE
        }
    }
}
