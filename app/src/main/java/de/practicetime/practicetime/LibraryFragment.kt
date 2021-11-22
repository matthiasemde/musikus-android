package de.practicetime.practicetime

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import de.practicetime.practicetime.entities.Category
import kotlinx.coroutines.launch


private var dao: PTDao? = null

class LibraryFragment : Fragment(R.layout.fragment_library) {

    private var editCategoryDialog: CategoryDialog? = null
    private val activeCategories = ArrayList<Category>()

    private var categoryAdapter : CategoryAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openDatabase()
        initCategoryList()
        editCategoryDialog = CategoryDialog(
            context = requireActivity(),
            ::editCategoryHandler,
            ::archiveCategoryHandler,
        )
    }

    private fun initCategoryList() {
        categoryAdapter = CategoryAdapter(
                lifecycleScope,
                dao,
                activeCategories,
                ::editCategory,
                context = requireActivity(),
                grow = true,
        )

        requireActivity().findViewById<RecyclerView>(R.id.libraryCategoryList).apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = categoryAdapter
        }

        // load all active categories from the database and notify the adapter
        lifecycleScope.launch {
            dao?.getActiveCategories()?.let { activeCategories.addAll(it) }
            categoryAdapter?.notifyItemRangeInserted(0, activeCategories.size)
        }
    }

    // the routine for handling presses to category buttons
    private fun editCategory(categoryView: View) {
        lifecycleScope.launch {
            val category = dao?.getCategory(id = categoryView.tag as Int)
            editCategoryDialog?.show(category)
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
    private fun archiveCategoryHandler(categoryId: Int) {
        lifecycleScope.launch {
            dao?.archiveCategory(categoryId)
            activeCategories.indexOfFirst { c -> c.id == categoryId }.also { i ->
                assert(i != -1)
                activeCategories.removeAt(i)
                categoryAdapter?.notifyItemRemoved(i)
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