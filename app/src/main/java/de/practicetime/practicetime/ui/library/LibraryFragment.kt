package de.practicetime.practicetime.ui.library

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.Category
import de.practicetime.practicetime.shared.setCommonToolbar
import kotlinx.coroutines.launch

class LibraryFragment : Fragment(R.layout.fragment_library) {

    private val activeCategories = ArrayList<Category>()
    private var categoryAdapter : CategoryAdapter? = null

    private var addCategoryDialog: CategoryDialog? = null
    private var editCategoryDialog: CategoryDialog? = null
    private var deleteCategoryDialog: AlertDialog? = null

    private lateinit var libraryToolbar: androidx.appcompat.widget.Toolbar
    private lateinit var libraryCollapsingToolbarLayout: CollapsingToolbarLayout

    private val selectedCategories = ArrayList<Int>()

    // catch the back press for the case where the selection should be reverted
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(selectedCategories.isNotEmpty()){
                    clearCategorySelection()
                    resetToolbar()
                }else{
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        initCategoryList()

        // create a new category dialog for adding new categories
        addCategoryDialog = CategoryDialog(requireActivity(), ::addCategoryHandler)

        view.findViewById<FloatingActionButton>(R.id.libraryFab).setOnClickListener {
            clearCategorySelection()
            resetToolbar()
            addCategoryDialog?.show()
        }

        // create the category dialog for editing categories
        editCategoryDialog = CategoryDialog(requireActivity(), ::editCategoryHandler)

        // create the dialog for archiving categories
        initDeleteCategoryDialog()

        libraryToolbar = view.findViewById(R.id.libraryToolbar)
        libraryCollapsingToolbarLayout = view.findViewById(R.id.library_collapsing_toolbar_layout)
        resetToolbar()  // initialize the toolbar with all its listeners
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
                activeCategories.addAll(it.reversed())
                categoryAdapter?.notifyItemRangeInserted(0, it.size)
            }

            requireActivity().findViewById<RecyclerView>(R.id.libraryCategoryList).apply {
                layoutManager = GridLayoutManager(context, 2)
                adapter = categoryAdapter
                itemAnimator?.apply {
                    addDuration = 500L
                    moveDuration = 500L
                    removeDuration = 200L
                }
            }

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

    private fun shortClickOnCategoryHandler(index: Int) {
        // if there are already categories selected,
        // add or remove the clicked category from the selection
        if(selectedCategories.isNotEmpty()) {
            if(selectedCategories.remove(index)) {
                categoryAdapter?.notifyItemChanged(index)
                if(selectedCategories.size == 1) {
                    libraryToolbar.menu.findItem(R.id.topToolbarSelectionEdit).isVisible = true
                } else if(selectedCategories.isEmpty()) {
                    resetToolbar()
                }
            } else {
                longClickOnCategoryHandler(index, vibrate = false)
            }
        } else {
            editCategoryDialog?.show(activeCategories[index])
        }
    }

    // the handler for dealing with long clicks on category
    private fun longClickOnCategoryHandler(index: Int, vibrate: Boolean = true): Boolean {
        // if there is no category selected already, change the toolbar
        if(selectedCategories.isEmpty()) {
            libraryToolbar.apply {
                // clear the base menu from the toolbar and inflate the new menu
                menu?.clear()
                inflateMenu(R.menu.library_toolbar_menu_for_selection)

                // set the back button and its click listener
                setNavigationIcon(R.drawable.ic_nav_back)
                setNavigationOnClickListener {
                    clearCategorySelection()
                    resetToolbar()
                }

                // set the click listeners for the menu options here
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.topToolbarSelectionDelete ->
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
                        R.id.topToolbarSelectionEdit -> {
                            editCategoryDialog?.show(activeCategories[selectedCategories.first()])
                        }
                    }
                    return@setOnMenuItemClickListener true
                }
            }
            // change the background color of the App Bar
            val typedValue = TypedValue()
            requireActivity().theme.resolveAttribute(R.attr.colorSurface, typedValue, true)
            val color = typedValue.data
            libraryCollapsingToolbarLayout.setBackgroundColor(color)
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

        libraryToolbar.menu.findItem(R.id.topToolbarSelectionEdit).isVisible =
            selectedCategories.size == 1

        // we consumed the event so we return true
        return true
    }

    private fun clearCategorySelection() {
        val tmpCopy = selectedCategories.toList()
        selectedCategories.clear()
        tmpCopy.forEach { categoryAdapter?.notifyItemChanged(it) }
    }

    // reset the toolbar and associated data
    private fun resetToolbar() {
        libraryToolbar.apply {
            menu?.clear()
            setCommonToolbar(requireActivity(), this) {
//                Place menu item click handler here
//                when(it) {
//                }
            }
            inflateMenu(R.menu.library_toolbar_menu_base)
            navigationIcon = null
        }
        libraryCollapsingToolbarLayout.background = null
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
            clearCategorySelection()
            resetToolbar()
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
            clearCategorySelection()
            resetToolbar()
        }
    }

    private fun showHint() {
        requireView().apply {
            findViewById<TextView>(R.id.libraryHint).visibility = View.VISIBLE
            findViewById<RecyclerView>(R.id.libraryCategoryList).visibility = View.GONE
        }
    }

    private fun hideHint() {
        requireView().apply {
            findViewById<TextView>(R.id.libraryHint).visibility = View.GONE
            findViewById<RecyclerView>(R.id.libraryCategoryList).visibility = View.VISIBLE
        }
    }
}