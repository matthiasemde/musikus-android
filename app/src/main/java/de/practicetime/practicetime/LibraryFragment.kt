package de.practicetime.practicetime

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
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

    private var editCategoryDialog: AlertDialog? = null

    private var categories = ArrayList<Category>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openDatabase()

        editCategoryDialog = createEditCategoryDialog()

        val categoryList : RecyclerView = view.findViewById(R.id.libraryCategoryList)
        initCategoryAdapter(categoryList)

    }

    private fun initCategoryAdapter(list: RecyclerView) {
        val categoryAdapter = CategoryAdapter(
                categories,
                ::editCategory,
                dao!!,
                context = requireActivity(),
                lifecycleScope,
                grow = true,
        )

        categoryAdapter.addCategoryDialog?.setOnDismissListener {
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

        list.layoutManager = GridLayoutManager(context, 2)
        list.adapter = categoryAdapter

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

    private fun createEditCategoryDialog(): AlertDialog {
        // instantiate the builder for the alert dialog
        val editCategoryDialogBuilder = AlertDialog.Builder(requireActivity())
        val inflater = this.layoutInflater;
        val dialogView = inflater.inflate(
                R.layout.dialog_view_add_or_change_category,
                null,
        )

        val categoryNameView = dialogView.findViewById<EditText>(R.id.addCategoryDialogName)
        val categoryColorButtonGroupRow1 =
                dialogView.findViewById<RadioGroup>(R.id.addCategoryDialogColorRow1)
        val categoryColorButtonGroupRow2 =
                dialogView.findViewById<RadioGroup>(R.id.addCategoryDialogColorRow2)

        val categoryColorButtons = listOf<RadioButton>(
                dialogView.findViewById(R.id.addCategoryDialogColor1),
                dialogView.findViewById(R.id.addCategoryDialogColor2),
                dialogView.findViewById(R.id.addCategoryDialogColor3),
                dialogView.findViewById(R.id.addCategoryDialogColor4),
                dialogView.findViewById(R.id.addCategoryDialogColor5),
                dialogView.findViewById(R.id.addCategoryDialogColor6),
                dialogView.findViewById(R.id.addCategoryDialogColor7),
                dialogView.findViewById(R.id.addCategoryDialogColor8),
                dialogView.findViewById(R.id.addCategoryDialogColor9),
                dialogView.findViewById(R.id.addCategoryDialogColor10),
        )

        var selectedColor: Int? = null

        var editCategoryDialog: AlertDialog? = null

        // Dialog Setup
        editCategoryDialogBuilder.apply {
            setView(dialogView)
            setPositiveButton(R.string.addCategoryAlertOkEdit) { dialog, _ ->
                val name = categoryNameView.text.toString().trim()
                if(name.isNotEmpty()) {
                    lifecycleScope.launch {
                        val updatedCategory = Category(
                                0,
                                name = name,
                                color = selectedColor!!,
                                archived = false,
                                profile_id = 0
                        )
                        dao?.updateCategory(updatedCategory)
                    }
                }
                dialog.dismiss()
            }
            setNegativeButton(R.string.addCategoryAlertCancel) { dialog, _ ->
                dialog.cancel()
            }
        }

        categoryColorButtons.forEachIndexed { index, button ->
            button.background.setTint(Color.parseColor(categoryColors[index]))
            button.setOnCheckedChangeListener { _, isChecked ->
                if(isChecked) {
                    selectedColor = Color.parseColor(categoryColors[index])
                    if (index < 5) {
                        categoryColorButtonGroupRow2.clearCheck()
                    } else {
                        categoryColorButtonGroupRow1.clearCheck()
                    }
                }
            }
        }

        return editCategoryDialogBuilder.create()
    }

    // the routine for handling presses to category buttons
    private fun editCategory(categoryView: View) {
//        editCategoryDialog?.show()
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }
}