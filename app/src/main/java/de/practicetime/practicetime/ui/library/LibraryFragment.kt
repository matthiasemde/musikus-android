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
import android.text.method.TextKeyListener.clear
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.util.TableInfo
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.composethemeadapter3.Mdc3Theme
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.Category
import de.practicetime.practicetime.shared.setCommonToolbar
import kotlinx.coroutines.launch

enum class LibrarySortMode {
    DATE_ADDED,
    LAST_MODIFIED,
    NAME,
    COLOR,
    CUSTOM
}

class LibraryFragment : Fragment(R.layout.fragment_library) {

    private val activeCategories = ArrayList<Category>()
    private var categoryAdapter : CategoryAdapter? = null

    private var addCategoryDialog: CategoryDialog? = null
    private var editCategoryDialog: CategoryDialog? = null
    private var deleteCategoryDialog: AlertDialog? = null

    private lateinit var libraryToolbar: MaterialToolbar
    private lateinit var libraryCollapsingToolbarLayout: CollapsingToolbarLayout

    private var actionMode: ActionMode? = null

    private lateinit var sortMode: LibrarySortMode
    private var sortDirection: Boolean = true

    private val selectedCategories = ArrayList<Int>()

    // catch the back press for the case where the selection should be reverted
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(selectedCategories.isNotEmpty()){
                    actionMode?.finish()
                }else{
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        })
    }

    @Composable
    fun LibraryItemComposable(
        category: Category,
//        itemClickedCallback: (callbackListDataItem: CallbackListDataItem) -> Unit,
    ) {
        AndroidView(
            factory = { context ->
                val view = LayoutInflater.from(context).inflate(R.layout.listitem_library_item, null)

                val cardView = view.findViewById<CardView>(R.id.library_item_card)
                val colorIndicatorView = view.findViewById<ImageView>(R.id.library_item_color_indicator)
                val colorOverlayView = view.findViewById<ImageView>(R.id.library_item_color_overlay)
                val nameView = view.findViewById<TextView>(R.id.library_item_name)

                cardView.isSelected = selectedCategories.contains(category.id.toInt())
                colorOverlayView.backgroundTintList = ColorStateList.valueOf(
                    PracticeTime.getCategoryColors(requireContext())[category.colorIndex]
                )
                colorIndicatorView.backgroundTintList = ColorStateList.valueOf(
                    PracticeTime.getCategoryColors(requireContext())[category.colorIndex]
                )

                nameView.text = category.name

                view // return the view
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            update = { view ->
                // Update the view
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        libraryToolbar = view.findViewById(R.id.libraryToolbar)
//        libraryCollapsingToolbarLayout = view.findViewById(R.id.libraryToolbarLayout)
//        initToolbar()  // initialize the toolbar with all its listeners


        lifecycleScope.launch {
            PracticeTime.categoryDao.get(activeOnly = true).let {
                // sort categories depending on the current sort mode
                activeCategories.addAll(it)
    //                                    sortCategoryList(sortMode)
            }
            Log.d("LibraryFragment", activeCategories.toString())
            view.findViewById<ComposeView>(R.id.library_appbar_compose).setContent {
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

                Mdc3Theme() {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                           LargeTopAppBar(
                                scrollBehavior = scrollBehavior,
                                title = {
                                    Text(text = "Library")
                                }
                            )
                        },
                        content = { contentPadding ->
                            LazyColumn(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .padding(horizontal = 8.dp),
                                contentPadding = contentPadding,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(activeCategories) { item ->
                                    LibraryItemComposable(
                                        category = item,
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        sortMode = try {
            LibrarySortMode.valueOf(
                PracticeTime.prefs.getString(
                    PracticeTime.PREFERENCES_KEY_LIBRARY_SORT_MODE,
                    LibrarySortMode.DATE_ADDED.toString()
                ) ?: LibrarySortMode.DATE_ADDED.toString()
            )
        } catch (ex: Exception) {
            LibrarySortMode.DATE_ADDED
        }

//        initCategoryList()

        // create a new category dialog for adding new categories
        addCategoryDialog = CategoryDialog(requireActivity(), ::addCategoryHandler)

        view.findViewById<FloatingActionButton>(R.id.libraryFab).setOnClickListener {
            actionMode?.finish()
            addCategoryDialog?.show()
        }

        // create the category dialog for editing categories
        editCategoryDialog = CategoryDialog(requireActivity(), ::editCategoryHandler)

        // create the dialog for archiving categories
        initDeleteCategoryDialog()
    }

    private fun sortCategoryList(mode: LibrarySortMode) {
        if(mode == sortMode) {
            sortDirection = !sortDirection
        } else {
            sortDirection = true
            sortMode = mode
            when (sortMode) {
                LibrarySortMode.DATE_ADDED -> activeCategories.sortBy { it.createdAt }
                LibrarySortMode.NAME -> activeCategories.sortBy { it.name }
                LibrarySortMode.COLOR -> activeCategories.sortBy { it.colorIndex }
                LibrarySortMode.LAST_MODIFIED -> activeCategories.sortBy { it.modifiedAt }
                LibrarySortMode.CUSTOM -> activeCategories.sortBy { it.createdAt } // TODO: Not implemented yet
            }
            val itemToSetIcon = when(sortMode) {
                LibrarySortMode.DATE_ADDED -> R.id.libraryToolbarSortModeDateAdded
                LibrarySortMode.LAST_MODIFIED -> R.id.libraryToolbarSortModeLastModified
                LibrarySortMode.NAME -> R.id.libraryToolbarSortModeName
                LibrarySortMode.COLOR -> R.id.libraryToolbarSortModeColor
                LibrarySortMode.CUSTOM -> R.id.libraryToolbarSortModeCustom
            }
            libraryToolbar.menu.findItem(itemToSetIcon).apply {
                val iconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_drop_down)!!
                // tint it like this because iconTintList requires API >=26
                DrawableCompat.setTint(iconDrawable, PracticeTime.getThemeColor(R.attr.colorOnSurfaceLowerContrast, requireContext()))
                icon = iconDrawable
            }
        }



        if(sortDirection) {
            activeCategories.reverse()
        }
        PracticeTime.prefs.edit().putString(
            PracticeTime.PREFERENCES_KEY_LIBRARY_SORT_MODE,
            sortMode.toString()
        ).apply()
//        categoryAdapter?.notifyItemRangeRemoved(0, activeCategories.size)
//        categoryAdapter?.notifyItemRangeInserted(0, activeCategories.size)
        categoryAdapter?.notifyItemRangeChanged(0, activeCategories.size)
    }

    private fun initCategoryList() {
        categoryAdapter = CategoryAdapter(
            activeCategories,
            selectedCategories,
            context = requireActivity(),
            shortClickHandler = ::shortClickOnCategoryHandler,
            longClickHandler = ::longClickOnCategoryHandler,
        )

        // load all active categories from the database and notify the adapter
        lifecycleScope.launch {
            PracticeTime.categoryDao.get(activeOnly = true).let {
                // sort categories depending on the current sort mode
                activeCategories.addAll(it)
                Log.d("LIBRARY","$sortMode")
                sortCategoryList(sortMode)
//                categoryAdapter?.notifyItemRangeInserted(0, it.size) // sortCategoryList() already notifies the adapter
            }

//            requireActivity().findViewById<RecyclerView>(R.id.libraryCategoryList).apply {
////                layoutManager = GridLayoutManager(context, 2)
//                layoutManager = LinearLayoutManager(context)
//                adapter = categoryAdapter
//                itemAnimator?.apply {
//                    addDuration = 500L
//                    moveDuration = 500L
//                    removeDuration = 200L
//                }
//            }

            if (activeCategories.isEmpty()) showHint()
        }
    }

    // initialize the category delete dialog
    private fun initDeleteCategoryDialog() {
        deleteCategoryDialog = AlertDialog.Builder(requireActivity()).apply {
            setPositiveButton(R.string.deleteDialogConfirm) { dialog, _ ->
                deleteCategoriesHandler()
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
                        editCategoryDialog?.show(activeCategories[selectedCategories.first()])
                        true
                    }
                    R.id.topToolbarSelectionDelete -> {
                        if(PracticeTime.serviceIsRunning)
                            Toast.makeText(context, getString(R.string.cannot_delete_error), Toast.LENGTH_SHORT).show()
                        else {
                            deleteCategoryDialog?.apply {
                                setMessage(
                                    context.getString(
                                        if (selectedCategories.size > 1) R.string.deleteCategoriesDialogMessage
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
        if(selectedCategories.isNotEmpty()) {
            if(selectedCategories.remove(index)) {
                categoryAdapter?.notifyItemChanged(index)
                if(selectedCategories.size == 1) {
                    actionMode?.menu?.findItem(R.id.topToolbarSelectionEdit)?.isVisible = true
                } else if(selectedCategories.isEmpty()) {
                    actionMode?.finish()
                }
            } else {
                longClickOnCategoryHandler(index, vibrate = false)
            }
            actionMode?.title = "${selectedCategories.size} selected"
        } else {
            editCategoryDialog?.show(activeCategories[index])
        }
    }

    // the handler for dealing with long clicks on category
    private fun longClickOnCategoryHandler(index: Int, vibrate: Boolean = true): Boolean {
        // if there is no category selected already, change the toolbar
        if(selectedCategories.isEmpty()) {
            actionMode = libraryToolbar.startActionMode(actionModeCallback())
            actionMode?.title = "1 selected"
        }

        if(!selectedCategories.contains(index)) {
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
            selectedCategories.add(index)
            categoryAdapter?.notifyItemChanged(index)
        }

        actionMode?.menu?.findItem(R.id.topToolbarSelectionEdit)?.isVisible =
            selectedCategories.size == 1

        // we consumed the event so we return true
        return true
    }

    private fun clearCategorySelection() {
        val tmpCopy = selectedCategories.toList()
        selectedCategories.clear()
        tmpCopy.forEach { categoryAdapter?.notifyItemChanged(it) }
    }

    // init the toolbar and associated data
    private fun initToolbar() {
        libraryToolbar.apply {
            menu?.clear()
            setCommonToolbar(requireActivity(), this) {
                when(it) {
                    R.id.libraryToolbarSortModeDateAdded -> sortCategoryList(LibrarySortMode.DATE_ADDED)
                    R.id.libraryToolbarSortModeLastModified -> sortCategoryList(LibrarySortMode.LAST_MODIFIED)
                    R.id.libraryToolbarSortModeName -> sortCategoryList(LibrarySortMode.NAME)
                    R.id.libraryToolbarSortModeColor -> sortCategoryList(LibrarySortMode.COLOR)
                    R.id.libraryToolbarSortModeCustom -> sortCategoryList(LibrarySortMode.CUSTOM)
                    else -> {}
                }
            }
            inflateMenu(R.menu.library_toolbar_menu_base)
//            setNavigationIcon(R.drawable.ic_account)
        }
    }

    // the handler for creating new categories
    private fun addCategoryHandler(newCategory: Category) {
        lifecycleScope.launch {
            PracticeTime.categoryDao.insertAndGet(newCategory)
                ?.let { activeCategories.add(0, it) }
            categoryAdapter?.notifyItemInserted(0)
            if(activeCategories.isNotEmpty()) hideHint()
        }
    }

    // the handler for editing categories
    private fun editCategoryHandler(category: Category) {
        lifecycleScope.launch {
            PracticeTime.categoryDao.update(category)
            activeCategories.indexOfFirst { c -> c.id == category.id }.also { i ->
                assert(i != -1) {
                    Log.e("EDIT_CATEGORY", "No category with matching id found for\n$category")
                }
                activeCategories[i] = category
                categoryAdapter?.notifyItemChanged(i)
            }
            actionMode?.finish()
        }
    }

    // the handler for deleting categories
    private fun deleteCategoriesHandler() {
        var failedDeleteFlag = false
        lifecycleScope.launch {
            selectedCategories.sortedByDescending { it }.forEach { index ->
                if(PracticeTime.categoryDao.archive(activeCategories[index].id)) {
                    activeCategories.removeAt(index)
                    categoryAdapter?.notifyItemRemoved(index)
                } else {
                    failedDeleteFlag = true
                }
            }

            if(failedDeleteFlag) {
                Snackbar.make(
                    requireView(),
                    if(selectedCategories.size > 1) R.string.deleteCategoriesFailSnackbar
                    else R.string.deleteCategoryFailSnackbar,
                    5000
                ).show()
            } else {
                Toast.makeText(
                    requireActivity(),
                    if(selectedCategories.size > 1) R.string.deleteCategoriesSuccessSnackbar
                    else R.string.deleteCategorySuccessSnackbar,
                    Toast.LENGTH_SHORT
                ).show()
            }

            if(activeCategories.isEmpty()) showHint()
            actionMode?.finish()
        }
    }

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
