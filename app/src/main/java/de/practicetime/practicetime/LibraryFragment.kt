package de.practicetime.practicetime

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openDatabase()
        initCategoryList()
        editCategoryDialog = CategoryDialog(
            context = requireActivity(),
            lifecycleScope,
            dao,
        )    }

    private fun initCategoryList() {
        val categories = ArrayList<Category>()
        val categoryAdapter = CategoryAdapter(
                lifecycleScope,
                dao,
                categories,
                ::editCategory,
                context = requireActivity(),
                grow = true,
        )

        categoryAdapter.addCategoryDialog?.alertDialog?.setOnDismissListener {
            lifecycleScope.launch {
                categories.clear()
                dao?.getActiveCategories().also {
                    if (it != null) {
                        categories.addAll(it)
                    }
                }
                // notifyDataSetChanged necessary here since all items might have changed
                categoryAdapter.notifyDataSetChanged()
            }
        }

        val categoryListView = requireActivity().findViewById<RecyclerView>(R.id.libraryCategoryList)
        categoryListView.layoutManager = GridLayoutManager(context, 2)
        categoryListView.adapter = categoryAdapter

        lifecycleScope.launch {
            dao?.getActiveCategories().also {
                if (it != null) {
                    categories.addAll(it)
                }
            }
            // notifyDataSetChanged necessary here since all items might have changed
            categoryAdapter.notifyDataSetChanged()
        }
    }

    // the routine for handling presses to category buttons
    private fun editCategory(categoryView: View) {
        lifecycleScope.launch {
            val category = dao?.getCategory(id = categoryView.tag as Int)
            editCategoryDialog?.show(category)
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