package de.practicetime.practicetime

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import de.practicetime.practicetime.entities.Category
import kotlinx.coroutines.launch


private var dao: PTDao? = null

class MetronomeFragment : Fragment(R.layout.fragment_library) {

     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openDatabase()

        val categoryList : RecyclerView = view.findViewById(R.id.libraryCategoryList)

        val categories = ArrayList<Category>()
        val categoryAdapter = CategoryAdapter(
            categories,
            ::categoryPressed,
            dao!!,
            grow = true,
            context = requireActivity()
        )

        categoryList.layoutManager = GridLayoutManager(context, 2)
        categoryList.adapter = categoryAdapter

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
    private fun categoryPressed(categoryView: View) {
        Log.d("Category", "Pressed ${categoryView.tag}")
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }
}