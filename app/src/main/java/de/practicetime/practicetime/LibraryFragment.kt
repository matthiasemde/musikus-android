package de.practicetime.practicetime

import android.app.AlertDialog
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import de.practicetime.practicetime.entities.Category
import kotlinx.coroutines.launch


private var dao: PTDao? = null

class LibraryFragment : Fragment(R.layout.fragment_library) {

    private val activeCategories = ArrayList<Category>()
    private var categoryAdapter : CategoryAdapter? = null

    private var addCategoryDialog: CategoryDialog? = null
    private var editCategoryDialog: CategoryDialog? = null
    private var deleteCategoryDialog: AlertDialog? = null

    private var libraryToolbar: androidx.appcompat.widget.Toolbar? = null
    private var libraryCollapsingToolbarLayout: CollapsingToolbarLayout? = null

    private val selectedCategories = ArrayList<Pair<Int, View>>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openDatabase()

        initCategoryList()

        // create a new category dialog for adding new categories
        addCategoryDialog = CategoryDialog(requireActivity(), ::addCategoryHandler)

        view.findViewById<FloatingActionButton>(R.id.libraryFab).setOnClickListener {
            resetToolbar()
            addCategoryDialog?.show()
        }

        // create the category dialog for editing categories
        editCategoryDialog = CategoryDialog(requireActivity(), ::editCategoryHandler)

        // create the dialog for archiving categories
        initDeleteCategoryDialog()

        libraryToolbar = view.findViewById(R.id.libraryToolbar)
        libraryCollapsingToolbarLayout = view.findViewById(R.id.library_collapsing_toolbar_layout)
    }

    private fun initCategoryList() {
        categoryAdapter = CategoryAdapter(
                activeCategories,
                context = requireActivity(),
                shortClickHandler = ::shortClickOnCategoryHandler,
                longClickHandler = ::longClickOnCategoryHandler,
        )

        // load all active categories from the database and notify the adapter
        lifecycleScope.launch {
            dao?.getActiveCategories()?.let {
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
        deleteCategoryDialog = requireActivity().let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setMessage(R.string.deleteCategoryDialogMessage)
                setPositiveButton(R.string.deleteDialogConfirm) { dialog, _ ->
                    deleteCategoriesHandler(selectedCategories.map{ p -> p.first})
                    dialog.dismiss()
                }
                setNegativeButton(R.string.dialogCancel) { dialog, _ ->
                    dialog.cancel()
                }
            }
            builder.create()
        }
    }

    private fun shortClickOnCategoryHandler(category: Category, categoryView: View) {
        // if there are already categories selected,
        // add or remove the clicked category from the selection
        if(selectedCategories.isNotEmpty()) {
            if(selectedCategories.remove(Pair(category.id, categoryView))) {
                setCategorySelected(false, categoryView)
                if(selectedCategories.isEmpty()) {
                    resetToolbar()
                }
            } else {
                longClickOnCategoryHandler(category.id, categoryView)
            }
        // if no selection is in progress show the edit dialog with the category
        } else {
            editCategoryDialog?.show(category)
        }
    }

    // the handler for dealing with long clicks on category
    private fun longClickOnCategoryHandler(categoryId: Int, categoryView: View): Boolean {
        // if there is no category selected already, change the toolbar
        if(selectedCategories.isEmpty()) {
            libraryToolbar?.apply {
                // clear the base menu from the toolbar and inflate the new menu
                menu?.clear()
                inflateMenu(R.menu.library_toolbar_menu_for_selection)

                // set the back button and its click listener
                setNavigationIcon(R.drawable.ic_nav_back)
                setNavigationOnClickListener {
                    resetToolbar()
                }

                // set the click listeners for the menu options here
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.topToolbarSelectionDelete -> deleteCategoryDialog?.show()
                    }
                    return@setOnMenuItemClickListener true
                }
            }
            // change the background color of the App Bar
            val typedValue = TypedValue()
            requireActivity().theme.resolveAttribute(R.attr.colorSurface, typedValue, true)
            var color = typedValue.data
            libraryCollapsingToolbarLayout?.setBackgroundColor(color)
        }

        // now add the newly selected category to the list...
        selectedCategories.add(Pair(categoryId, categoryView))

        // and tint its foreground to mark it as selected
        setCategorySelected(true, categoryView)

        // we consumed the event so we return true
        return true
    }

    // reset the toolbar and associated data
    private fun resetToolbar() {
        libraryToolbar?.apply {
            menu?.clear()
            inflateMenu(R.menu.library_toolbar_menu_base)
            navigationIcon = null
        }
        libraryCollapsingToolbarLayout?.apply {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        }
        for ((_, view) in selectedCategories) {
            setCategorySelected(false, view)
        }
        selectedCategories.clear()
    }

    private fun setCategorySelected(selected: Boolean, view: View) {
        (view as Button).isSelected = selected // set selected so that background changes
    }

    // the handler for creating new categories
    private fun addCategoryHandler(newCategory: Category) {
        lifecycleScope.launch {
            val newCategoryId = dao?.insertCategory(newCategory)?.toInt()
            if(newCategoryId != null) {
                // we need to fetch the newly created category to get the correct id
                dao?.getCategory(newCategoryId)?.let { activeCategories.add(0, it) }
                categoryAdapter?.notifyItemInserted(0)
            }
            if(activeCategories.isNotEmpty()) hideHint()
        }
    }

    // the handler for editing categories
    private fun editCategoryHandler(category: Category) {
        lifecycleScope.launch {
            dao?.insertCategory(category)
            activeCategories.indexOfFirst { c -> c.id == category.id }.also { i ->
                assert(i != -1)
                activeCategories[i] = category
                categoryAdapter?.notifyItemChanged(i)
            }
        }
    }

    // the handler for deleting categories
    private fun deleteCategoriesHandler(categoryIds: List<Int>) {
        var failedDeleteFlag = false
        lifecycleScope.launch {
            for (categoryId in categoryIds) {
                if(dao?.deleteCategory(categoryId) == true) {
                    activeCategories.indexOfFirst { category ->
                        category.id == categoryId
                    }.also { index ->
                        activeCategories.removeAt(index)
                        categoryAdapter?.notifyItemRemoved(index)
                    }
                } else {
                    failedDeleteFlag = true
                }
            }
            if(failedDeleteFlag) {
                Snackbar.make(
                    requireView(),
                    if(categoryIds.size > 1) R.string.deleteCategoriesFailSnackbar
                    else R.string.deleteCategoryFailSnackbar,
                    5000
                ).show()
            } else {
                Toast.makeText(
                    requireActivity(),
                    if(categoryIds.size > 1) R.string.deleteCategoriesSuccessSnackbar
                    else R.string.deleteCategorySuccessSnackbar,
                    Toast.LENGTH_SHORT
                ).show()
            }
            if(activeCategories.isEmpty()) showHint()
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

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }
}