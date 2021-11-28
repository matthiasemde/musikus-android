package de.practicetime.practicetime

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.practicetime.practicetime.entities.Category
import kotlinx.coroutines.launch


private var dao: PTDao? = null

class LibraryFragment : Fragment(R.layout.fragment_library) {

    private val activeCategories = ArrayList<Category>()
    private var categoryAdapter : CategoryAdapter? = null

    private var addCategoryDialog: CategoryDialog? = null
    private var editCategoryDialog: CategoryDialog? = null
    private var archiveCategoryDialog: AlertDialog? = null

    private var libraryToolbar: androidx.appcompat.widget.Toolbar? = null

    private val selectedCategories = ArrayList<Pair<Int, View>>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openDatabase()

        initCategoryList()

        // create the category dialog for adding or editing categories
        editCategoryDialog = CategoryDialog(
            context = requireActivity(),
            ::editCategoryHandler,
        )

        view.findViewById<FloatingActionButton>(R.id.libraryFab).setOnClickListener {
            resetToolbar()
            addCategoryDialog?.show()
        }

        initArchiveCategoryDialog()

        libraryToolbar = view.findViewById(R.id.libraryToolbar)
    }

    private fun initCategoryList() {
        categoryAdapter = CategoryAdapter(
                activeCategories,
                context = requireActivity(),
                shortClickHandler = ::shortClickOnCategoryHandler,
                longClickHandler = ::longClickOnCategoryHandler,
        )

        requireActivity().findViewById<RecyclerView>(R.id.libraryCategoryList).apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = categoryAdapter
            itemAnimator?.apply {
                addDuration = 500L
                moveDuration = 500L
                removeDuration = 200L
            }
        }

        // load all active categories from the database and notify the adapter
        lifecycleScope.launch {
            dao?.getActiveCategories()?.let { activeCategories.addAll(it.reversed()) }
            categoryAdapter?.notifyItemRangeInserted(0, activeCategories.size)
        }

        // the handler for creating new categories
        fun addCategoryHandler(newCategory: Category) {
            lifecycleScope.launch {
                val newCategoryId = dao?.insertCategory(newCategory)?.toInt()
                if(newCategoryId != null) {
                    // we need to fetch the newly created category to get the correct id
                    dao?.getCategory(newCategoryId)?.let { activeCategories.add(0, it) }
                    categoryAdapter?.notifyItemInserted(0)
                }
            }
        }

        // create a new category dialog for adding new categories
        addCategoryDialog = CategoryDialog(requireActivity(), ::addCategoryHandler)
    }

    // initialize the category archive dialog
    private fun initArchiveCategoryDialog() {
        archiveCategoryDialog = requireActivity().let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setTitle(R.string.archiveCategoryDialogTitle)
                setPositiveButton(R.string.archiveDialogConfirm) { dialog, _ ->
                    lifecycleScope.launch {
                        selectedCategories.forEach { (categoryId, _) ->
                            dao?.archiveCategory(categoryId)
                            activeCategories.indexOfFirst { category ->
                                category.id == categoryId
                            }.also { index ->
                                activeCategories.removeAt(index)
                                categoryAdapter?.notifyItemRemoved(index)
                            }
                        }
                        Toast.makeText(context, R.string.archiveCategoryToast, Toast.LENGTH_SHORT).show()
                        resetToolbar()
                    }
                    dialog.dismiss()
                }
                setNegativeButton(R.string.archiveDialogCancel) { dialog, _ ->
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
                categoryView.foregroundTintList = null
                if(selectedCategories.isEmpty()) {
                    resetToolbar()
                }
            } else {
                longClickOnCategoryHandler(category.id, categoryView)
            }
        // if no selection is in progress show the dit dialog with the category
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
                        R.id.topToolbarSelectionArchive -> archiveCategoryDialog?.show()
                    }
                    return@setOnMenuItemClickListener true
                }
            }
        }

        // now add the newly selected category to the list...
        selectedCategories.add(Pair(categoryId, categoryView))

        // and tint its foreground to mark it as selected
        categoryView.foregroundTintList = ColorStateList.valueOf(
            requireActivity().resources.getColor(R.color.redTransparent, requireActivity().theme)
        )

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
        for ((_, view) in selectedCategories) {
            view.foregroundTintList = null
        }
        selectedCategories.clear()
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

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }
}